package com.swu.smartgrade.grading.application;

import com.swu.smartgrade.common.api.BusinessException;
import com.swu.smartgrade.common.storage.ObjectStorage;
import com.swu.smartgrade.grading.domain.GradingResult;
import com.swu.smartgrade.grading.domain.GradingResultRepository;
import com.swu.smartgrade.grading.domain.HitEvidence;
import com.swu.smartgrade.grading.domain.HitEvidenceRepository;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.rendering.PDFRenderer;
import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.Arrays;
import com.swu.smartgrade.grading.api.dto.MatchRequest;
import com.swu.smartgrade.grading.api.dto.MatchResult;
import com.swu.smartgrade.grading.api.dto.GradingCapabilities;
import com.swu.smartgrade.grading.api.dto.OcrRequest;
import com.swu.smartgrade.grading.api.dto.OcrResult;
import com.swu.smartgrade.grading.api.dto.RetryRequest;
import com.swu.smartgrade.grading.api.dto.TaskView;
import com.swu.smartgrade.grading.config.ScoringProperties;
import com.swu.smartgrade.grading.domain.GradingTask;
import com.swu.smartgrade.grading.domain.GradingTaskRepository;
import com.swu.smartgrade.grading.infrastructure.ocr.VisionLlmOcrClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Locale;

@Service
public class GradingApplicationService {
    private final GradingTaskRepository repository;
    private final ExplainableMatcher matcher;
    private final ScoringProperties properties;
    private final VisionLlmOcrClient visionOcrClient;
    private final ObjectStorage storage;
    private final GradingResultRepository resultRepository;
    private final HitEvidenceRepository evidenceRepository;

    public GradingApplicationService(GradingTaskRepository repository,
                                     ExplainableMatcher matcher,
                                     ScoringProperties properties,
                                     VisionLlmOcrClient visionOcrClient,
                                     ObjectStorage storage,
                                     GradingResultRepository resultRepository,
                                     HitEvidenceRepository evidenceRepository) {
        this.repository = repository;
        this.matcher = matcher;
        this.properties = properties;
        this.visionOcrClient = visionOcrClient;
        this.storage = storage;
        this.resultRepository = resultRepository;
        this.evidenceRepository = evidenceRepository;
    }

    @Transactional
    public OcrResult ocr(String taskId, OcrRequest request) {
        long start = System.currentTimeMillis();
        GradingTask task = task(taskId);
        if ("MANUAL_REVIEW".equals(task.getStatus())) {
            throw BusinessException.conflict("任务已达到重试上限，请人工复核");
        }
        task.setAnswerId(request.answerId());
        task.setStatus("OCR_RUNNING");
        task.setUpdatedAt(LocalDateTime.now());
        repository.save(task);
        try {
            RecognizedAnswer recognized = recognize(request);
            task.setNormalizedText(recognized.text());
            task.setConfidence(recognized.confidence());
            task.setStatus("OCR_COMPLETED");
            task.setErrorCode(null);
            repository.save(task);
            return new OcrResult(recognized.text(), recognized.confidence(), recognized.coverageRatio(),
                    recognized.characterCount(), recognized.provider(), null,
                    System.currentTimeMillis() - start);
        } catch (Exception ex) {
            fail(task, "OCR_FAILED");
            return new OcrResult(null, BigDecimal.ZERO, BigDecimal.ZERO, 0, "unknown",
                    ex.getMessage(), System.currentTimeMillis() - start);
        }
    }

    @Transactional
    public MatchResult match(String taskId, MatchRequest request) {
        GradingTask task = task(taskId);
        MatchResult result = matcher.match(request.text(), request.points(),
                request.answerCoverageRatio(), request.maxScore(), request.baseScore());
        task.setStatus("WAITING_REVIEW");
        task.setRubricVersion(request.rubricVersion());
        task.setIdempotencyKey(taskId + ":" + request.rubricVersion());
        task.setConfidence(result.confidence());
        repository.save(task);
        GradingResult stored = resultRepository.findByTaskId(taskId).orElseGet(GradingResult::new);
        if (stored.getResultId() == null) {
            stored.setResultId("GR-" + UUID.randomUUID());
            stored.setCreatedAt(LocalDateTime.now());
        }
        stored.setTaskId(taskId);
        stored.setAnswerId(task.getAnswerId());
        stored.setRubricVersion(request.rubricVersion());
        stored.setSuggestedScore(result.suggestedScore());
        stored.setBaseScore(com.swu.smartgrade.common.util.ScorePolicy.baseScore(request.maxScore(), request.baseScore()));
        stored.setConfidence(result.confidence());
        stored.setStatus("WAITING_REVIEW");
        resultRepository.save(stored);
        evidenceRepository.deleteAll(evidenceRepository.findByResultId(stored.getResultId()));
        for (var hit : result.hits()) {
            HitEvidence evidence = new HitEvidence();
            evidence.setEvidenceId("HE-" + UUID.randomUUID());
            evidence.setResultId(stored.getResultId());
            evidence.setPointId(hit.pointId());
            evidence.setPointName(hit.pointName());
            evidence.setMatchType(hit.matchType());
            evidence.setSentence(hit.evidence());
            evidence.setStartOffset(hit.start());
            evidence.setEndOffset(hit.end());
            evidence.setConfidence(hit.confidence());
            evidenceRepository.save(evidence);
        }
        return result;
    }

    @Transactional
    public TaskView retry(String taskId, RetryRequest request) {
        GradingTask task = repository.findForUpdate(taskId)
                .orElseThrow(() -> BusinessException.notFound("初筛任务不存在"));
        String key = request.idempotencyKey();
        if (key == null || key.isBlank()) {
            key = request.retryReason();
        }
        if (key == null || key.isBlank() || key.length() > 255 || key.contains("\n")) {
            throw BusinessException.badRequest("重试必须提供 1 至 255 字符的幂等键");
        }
        String keys = task.getRetryKeys() == null ? "" : task.getRetryKeys();
        if (Arrays.asList(keys.split("\n")).contains(key)) {
            return view(task);
        }
        if (task.getRetryCount() >= 3) {
            task.setStatus("MANUAL_REVIEW");
            task.setErrorCode("RETRY_EXHAUSTED");
            repository.save(task);
            return view(task);
        }
        if (!"FAILED".equals(task.getStatus())) {
            throw BusinessException.conflict("仅失败任务可以重试");
        }
        task.setRetryCount(task.getRetryCount() + 1);
        task.setRetryKeys(keys + key + "\n");
        task.setIdempotencyKey(key);
        task.setUpdatedAt(LocalDateTime.now());
        task.setStatus("PENDING");
        task.setErrorCode(null);
        repository.save(task);
        return view(task);
    }

    @Transactional(readOnly = true)
    public TaskView get(String taskId) {
        return view(repository.findById(taskId)
                .orElseThrow(() -> BusinessException.notFound("初筛任务不存在")));
    }

    public GradingCapabilities capabilities() {
        return new GradingCapabilities(properties.getBaseScore(), properties.getMinAnswerAreaRatio(),
                properties.getMinimumTextCharacters(), properties.isRoundToInteger(),
                properties.getOcr().getProvider(), properties.getOcr().getModel(),
                properties.getOcr().isConfigured());
    }

    private void fail(GradingTask task, String errorCode) {
        task.setStatus(task.getRetryCount() >= 3 ? "MANUAL_REVIEW" : "FAILED");
        task.setErrorCode(errorCode);
        repository.save(task);
    }

    private RecognizedAnswer recognize(OcrRequest request) throws Exception {
        String suppliedText = request.text();
        if (suppliedText != null && !suppliedText.isBlank()) {
            return textAnswer(suppliedText);
        }
        if (request.objectKey() == null || request.objectKey().isBlank()) {
            throw BusinessException.badRequest("答卷文件缺少存储路径");
        }
        String filename = request.objectKey().toLowerCase(Locale.ROOT);
        byte[] bytes = storage.read(request.objectKey());
        if (filename.endsWith(".txt") || filename.endsWith(".md")) {
            return textAnswer(new String(bytes, StandardCharsets.UTF_8));
        }
        if (filename.endsWith(".pdf")) {
            return recognizePdf(bytes);
        }
        if (!(filename.endsWith(".jpg") || filename.endsWith(".jpeg")
                || filename.endsWith(".png") || filename.endsWith(".webp"))) {
            throw BusinessException.badRequest("仅支持 JPG、PNG、WEBP、TXT、MD 或 PDF 答卷");
        }
        VisionLlmOcrClient.RecognizedDocument document = visionOcrClient.recognize(bytes, filename);
        return new RecognizedAnswer(document.text(), document.confidence(),
                document.answerAreaRatio(), document.characterCount(), document.provider());
    }

    private RecognizedAnswer recognizePdf(byte[] bytes) throws Exception {
        try (var document = Loader.loadPDF(bytes)) {
            if (document.isEncrypted() || document.getNumberOfPages() == 0 || document.getNumberOfPages() > 20) {
                throw BusinessException.badRequest("PDF 必须为未加密的 1 至 20 页答卷");
            }
            StringBuilder text = new StringBuilder();
            BigDecimal confidence = BigDecimal.ONE;
            BigDecimal coverage = BigDecimal.ZERO;
            PDFRenderer renderer = new PDFRenderer(document);
            PDFTextStripper stripper = new PDFTextStripper();
            boolean usedVision = false;
            for (int page = 0; page < document.getNumberOfPages(); page++) {
                stripper.setStartPage(page + 1);
                stripper.setEndPage(page + 1);
                String pageText = stripper.getText(document).trim();
                RecognizedAnswer recognized;
                // Image-bearing pages are rendered, so printed headings cannot hide handwriting.
                boolean images = false;
                var resources = document.getPage(page).getResources();
                if (resources != null) {
                    for (var name : resources.getXObjectNames()) {
                        images |= resources.isImageXObject(name);
                    }
                }
                if (pageText.isBlank() || images) {
                    ByteArrayOutputStream image = new ByteArrayOutputStream();
                    ImageIO.write(renderer.renderImageWithDPI(page, 120), "png", image);
                    var vision = visionOcrClient.recognize(image.toByteArray(), "page.png");
                    recognized = new RecognizedAnswer(vision.text(), vision.confidence(),
                            vision.answerAreaRatio(), vision.characterCount(), vision.provider());
                    usedVision = true;
                } else {
                    recognized = textAnswer(pageText);
                }
                text.append(recognized.text()).append('\n');
                confidence = confidence.min(recognized.confidence());
                coverage = coverage.add(recognized.coverageRatio());
            }
            String combined = text.toString().trim();
            return new RecognizedAnswer(combined, confidence,
                    coverage.divide(BigDecimal.valueOf(document.getNumberOfPages()), 2, RoundingMode.HALF_UP),
                    combined.replaceAll("\\s+", "").length(), usedVision ? "pdf-vision" : "pdf-text");
        }
    }

    private RecognizedAnswer textAnswer(String text) {
        String safeText = text == null ? "" : text.trim();
        int characters = safeText.replaceAll("\\s+", "").length();
        BigDecimal ratio = BigDecimal.ZERO;
        // Text files do not have a paper image, so character fullness is the reproducible fallback metric.
        int minimum = Math.max(1, properties.getMinimumTextCharacters());
        if (!safeText.isBlank()) {
            ratio = BigDecimal.valueOf(Math.min(characters, minimum))
                    .divide(BigDecimal.valueOf(minimum), 2, RoundingMode.HALF_UP);
        }
        return new RecognizedAnswer(safeText, new BigDecimal("0.99"), ratio, characters, "text-file");
    }

    private GradingTask task(String taskId) {
        return repository.findById(taskId).orElseGet(() -> {
            GradingTask created = new GradingTask();
            created.setTaskId(taskId);
            created.setStatus("PENDING");
            created.setRetryCount(0);
            created.setCreatedAt(LocalDateTime.now());
            created.setUpdatedAt(LocalDateTime.now());
            return created;
        });
    }

    private TaskView view(GradingTask task) {
        return new TaskView(task.getTaskId(), task.getAnswerId(), task.getStatus(),
                task.getRetryCount(), task.getErrorCode());
    }

    private record RecognizedAnswer(
            String text,
            BigDecimal confidence,
            BigDecimal coverageRatio,
            int characterCount,
            String provider
    ) {
    }
}
