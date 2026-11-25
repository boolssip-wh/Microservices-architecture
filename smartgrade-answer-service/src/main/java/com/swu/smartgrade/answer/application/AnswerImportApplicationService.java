package com.swu.smartgrade.answer.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.swu.smartgrade.answer.api.dto.ImportResult;
import com.swu.smartgrade.answer.api.dto.ValidationView;
import com.swu.smartgrade.answer.domain.Answer;
import com.swu.smartgrade.answer.domain.AnswerRepository;
import com.swu.smartgrade.answer.domain.Batch;
import com.swu.smartgrade.answer.domain.BatchRepository;
import com.swu.smartgrade.answer.domain.ImportJob;
import com.swu.smartgrade.answer.domain.ImportJobRepository;
import com.swu.smartgrade.common.storage.ObjectStorage;
import com.swu.smartgrade.answer.infrastructure.client.RubricClient;
import com.swu.smartgrade.common.api.BusinessException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class AnswerImportApplicationService {
    private final BatchRepository batchRepository;
    private final ImportJobRepository jobRepository;
    private final AnswerRepository answerRepository;
    private final ObjectStorage storage;
    private final RubricClient rubrics;
    private final ImportFileReader reader;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    public AnswerImportApplicationService(BatchRepository batchRepository,
                                          ImportJobRepository jobRepository,
                                          AnswerRepository answerRepository,
                                          ObjectStorage storage,
                                          ObjectMapper objectMapper,
                                          ApplicationEventPublisher eventPublisher, RubricClient rubrics, ImportFileReader reader) {
        this.batchRepository = batchRepository;
        this.jobRepository = jobRepository;
        this.answerRepository = answerRepository;
        this.storage = storage;
        this.objectMapper = objectMapper;
        this.eventPublisher = eventPublisher;
        this.rubrics = rubrics;
        this.reader = reader;
    }

    @Transactional
    @SentinelResource(value = "answer-import", blockHandler = "onImportBlocked")
    public ImportResult importAnswers(Long teacherId, String batchId, List<MultipartFile> files,
                                      String sourceType, String numberingRule, String idempotencyKey) {
        if (idempotencyKey == null || !idempotencyKey.matches("[A-Za-z0-9_-]{8,64}")) {
            throw BusinessException.badRequest("Idempotency-Key 须为 8 至 64 位字母数字连字符下划线");
        }
        if (!"filename".equalsIgnoreCase(numberingRule)) {
            throw BusinessException.badRequest("编号规则须为 filename");
        }
        Batch batch = batchRepository.lockById(batchId)
                .orElseThrow(() -> BusinessException.notFound("批次不存在"));
        if (!teacherId.equals(batch.getTeacherId())) {
            throw BusinessException.forbidden("不能向其他教师的批次导入答卷");
        }
        var previous = jobRepository.findByBatchIdAndIdempotencyKey(batchId, idempotencyKey);
        if (previous.isPresent()) {
            ImportJob saved = previous.get();
            return new ImportResult(saved.getJobId(), saved.getAcceptedCount(), saved.getRejectedCount(), saved.getStatus());
        }
        var rubric = rubrics.current(batchId).data();
        if (rubric == null || rubric.rubricVersion() < 1) throw BusinessException.badRequest("导入前须冻结评分规则");
        var inputs = reader.read(files, sourceType);
        ImportJob job = new ImportJob();
        job.setJobId(id("J"));
        job.setBatchId(batchId);
        job.setIdempotencyKey(idempotencyKey);
        try { job.setRubricSnapshotJson(objectMapper.writeValueAsString(rubric)); }
        catch (Exception ex) { throw new IllegalStateException("无法绑定冻结规则快照", ex); }
        job.setSourceType(sourceType);
        job.setNumberingRule(numberingRule);
        job.setStatus("VALIDATING");
        job.setAcceptedCount(0);
        job.setRejectedCount(0);
        job.setCreatedAt(LocalDateTime.now());
        jobRepository.save(job);

        List<String> duplicates = new ArrayList<>();
        List<String> blank = new ArrayList<>();
        List<String> corrupted = new ArrayList<>();
        Set<String> numbers = new HashSet<>();
        Set<String> hashes = new HashSet<>();
        for (Answer old : answerRepository.findByBatchIdOrderByStudentNo(batchId)) {
            numbers.add(old.getStudentNo());
            if (old.getContentHash() != null) hashes.add(old.getContentHash());
        }
        int accepted = 0;
        for (ImportFileReader.InputFile file : inputs) {
            String studentNo = studentNo(file.name());
            String detectedSourceType = file.sourceType();
            byte[] bytes = file.bytes();
            if (!studentNo.matches("[A-Za-z0-9_-]{1,64}")) { corrupted.add(file.name()); continue; }
            if (bytes.length == 0) { blank.add(file.name()); continue; }
            if (!reader.readable(file)) { corrupted.add(file.name()); continue; }
            String hash = contentHash(bytes);
            if (numbers.contains(studentNo) || hashes.contains(hash)) {
                duplicates.add(studentNo);
                continue;
            }
            if (detectedSourceType == null) {
                corrupted.add(file.name());
                continue;
            }
            String text = null;
            try { if ("TEXT".equals(detectedSourceType)) text = reader.text(file); }
            catch (Exception ex) { corrupted.add(file.name()); continue; }
            if (text != null && text.isBlank()) {
                blank.add(studentNo);
                continue;
            }
            Answer answer = new Answer();
            answer.setAnswerId(id("A"));
            answer.setBatchId(batchId);
            answer.setImportJobId(job.getJobId());
            answer.setStudentNo(studentNo);
            answer.setSourceType(detectedSourceType);
            answer.setObjectKey(storage.store("answers/" + answer.getAnswerId() + "/" + file.name(), bytes,
                    "TEXT".equals(detectedSourceType) ? "text/plain" : "PDF".equals(detectedSourceType) ? "application/pdf" : "application/octet-stream"));
            answer.setContentHash(hash);
            answer.setOcrText(text);
            answer.setStatus("PENDING");
            answer.setCreatedAt(LocalDateTime.now());
            answerRepository.save(answer);
            numbers.add(studentNo);
            hashes.add(hash);
            accepted++;
        }
        job.setAcceptedCount(accepted);
        job.setRejectedCount(inputs.size() - accepted);
        job.setValidationJson(writeValidation(duplicates, blank, corrupted, accepted == 0 ? "FAILED" : accepted == inputs.size() ? "PASSED" : "PARTIAL_FAILED"));
        job.setStatus(accepted == 0 ? "FAILED" : "PENDING");
        jobRepository.save(job);
        if (accepted > 0) {
            batch.setStatus("PROCESSING");
            batchRepository.save(batch);
            eventPublisher.publishEvent(new ImportReadyEvent(job.getJobId()));
        }
        return new ImportResult(job.getJobId(), accepted, inputs.size() - accepted, job.getStatus());
    }

    public ImportResult onImportBlocked(Long teacherId, String batchId, List<MultipartFile> files,
                                        String sourceType, String numberingRule, String idempotencyKey, BlockException exception) {
        throw BusinessException.tooManyRequests("答卷导入请求过于集中，Sentinel 已开启流量保护，请稍后重试");
    }

    @Transactional(readOnly = true)
    public ValidationView validation(Long teacherId, String jobId) {
        ImportJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> BusinessException.notFound("导入任务不存在"));
        Batch batch = batchRepository.findById(job.getBatchId()).orElseThrow();
        if (!teacherId.equals(batch.getTeacherId())) throw BusinessException.forbidden("不能访问其他教师的导入任务");
        try {
            ValidationView stored = objectMapper.readValue(job.getValidationJson(), ValidationView.class);
            return stored;
        } catch (Exception ex) {
            throw new IllegalStateException("导入校验结果损坏", ex);
        }
    }

    private String writeValidation(List<String> duplicates, List<String> blank, List<String> corrupted, String status) {
        try {
            return objectMapper.writeValueAsString(new ValidationView(duplicates, blank, corrupted, status));
        } catch (Exception ex) {
            throw new IllegalStateException("无法保存校验结果", ex);
        }
    }

    private String studentNo(String filename) {
        String name = filename == null ? "UNKNOWN" : filename;
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    private String detectSourceType(String filename) {
        String name = filename == null ? "" : filename.toLowerCase(Locale.ROOT);
        if (name.endsWith(".txt") || name.endsWith(".md")) {
            return "TEXT";
        }
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")
                || name.endsWith(".png") || name.endsWith(".webp")) {
            return "IMAGE";
        }
        return null;
    }

    private String id(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }

    private String contentHash(byte[] bytes) {
        try { return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (Exception ex) { throw new IllegalStateException(ex); }
    }
}
