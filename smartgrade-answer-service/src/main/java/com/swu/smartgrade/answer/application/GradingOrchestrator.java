package com.swu.smartgrade.answer.application;

import com.swu.smartgrade.answer.domain.Answer;
import com.swu.smartgrade.answer.domain.AnswerRepository;
import com.swu.smartgrade.answer.domain.Batch;
import com.swu.smartgrade.answer.domain.BatchRepository;
import com.swu.smartgrade.answer.domain.ImportJob;
import com.swu.smartgrade.answer.domain.ImportJobRepository;
import com.swu.smartgrade.answer.infrastructure.client.GradingClient;
import com.swu.smartgrade.answer.infrastructure.client.ReviewClient;
import com.swu.smartgrade.answer.infrastructure.client.RubricClient;
import com.swu.smartgrade.common.api.BusinessException;
import com.swu.smartgrade.common.cache.IdempotencyStore;
import com.swu.smartgrade.common.event.DomainEvent;
import com.swu.smartgrade.common.event.DomainEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;

@Component
public class GradingOrchestrator {
    private static final Logger log = LoggerFactory.getLogger(GradingOrchestrator.class);
    private final ImportJobRepository jobRepository;
    private final AnswerRepository answerRepository;
    private final BatchRepository batchRepository;
    private final RubricClient rubricClient;
    private final GradingClient gradingClient;
    private final ReviewClient reviewClient;
    private final DomainEventPublisher eventPublisher;
    private final IdempotencyStore guards;

    public GradingOrchestrator(ImportJobRepository jobRepository,
                               AnswerRepository answerRepository,
                               BatchRepository batchRepository,
                               RubricClient rubricClient,
                               GradingClient gradingClient,
                               ReviewClient reviewClient,
                               DomainEventPublisher eventPublisher, IdempotencyStore guards) {
        this.jobRepository = jobRepository;
        this.answerRepository = answerRepository;
        this.batchRepository = batchRepository;
        this.rubricClient = rubricClient;
        this.gradingClient = gradingClient;
        this.reviewClient = reviewClient;
        this.eventPublisher = eventPublisher;
        this.guards = guards;
    }

    public void process(ImportReadyEvent event) {
        String guard = "import-processing:" + event.jobId();
        if (!guards.putIfAbsent(guard, "running", java.time.Duration.ofMinutes(30))) return;
        try { processJob(event); }
        finally { guards.delete(guard); }
    }

    private void processJob(ImportReadyEvent event) {
        ImportJob job = jobRepository.findById(event.jobId()).orElseThrow();
        if (List.of("COMPLETED", "PARTIAL_FAILED", "FAILED").contains(job.getStatus())) return;
        Batch batch = batchRepository.findById(job.getBatchId()).orElseThrow();
        job.setStatus("RUNNING");
        jobRepository.save(job);
        try {
            RubricClient.RemoteRubric rubric = boundRubric(job);
            if (rubric == null) {
                throw BusinessException.unavailable("评分规则服务暂时不可用，已触发服务降级");
            }
            List<Answer> answers = answerRepository.findByImportJobIdOrderByStudentNo(job.getJobId());
            for (Answer answer : answers) {
                if (List.of("PENDING", "RUNNING", "FAILED").contains(answer.getStatus())) processOne(job, batch, rubric, answer);
            }
            boolean failures = answers.stream().anyMatch(a -> List.of("FAILED", "MANUAL_REVIEW").contains(a.getStatus()));
            job.setStatus(failures ? "PARTIAL_FAILED" : "COMPLETED");
            batch.setStatus("REVIEWING");
            safeNotify(batch, failures ? "grading.failed" : "grading.completed", "初筛处理结束",
                    failures ? "部分答卷需人工处理，请查看复核清单。" : "答卷已完成初筛，请复核。");
        } catch (Exception ex) {
            log.error("批次 {} 初筛编排失败", batch.getBatchId(), ex);
            // Every accepted answer must remain reachable through the failed-task retry API.
            for (Answer answer : answerRepository.findByImportJobIdOrderByStudentNo(job.getJobId())) {
                if (List.of("PENDING", "RUNNING").contains(answer.getStatus())) {
                    answer.setStatus("FAILED"); answerRepository.save(answer);
                }
            }
            job.setStatus("FAILED");
            batch.setStatus("FAILED");
            safeNotify(batch, "grading.failed", "初筛失败", ex.getMessage());
        } finally {
            job.setFinishedAt(LocalDateTime.now());
            jobRepository.save(job);
            batchRepository.save(batch);
        }
    }

    private void processOne(ImportJob job, Batch batch, RubricClient.RemoteRubric rubric, Answer answer) {
        for (int attempt = 0; attempt <= 3; attempt++) {
            if (attempt > 0) {
                try {
                    var state = gradingClient.get("TASK-" + answer.getAnswerId()).data();
                    // A successful match awaiting C05 delivery does not consume OCR retries.
                    if (state == null) break;
                    if (!"WAITING_REVIEW".equals(state.status())) {
                        var response = gradingClient.retry("TASK-" + answer.getAnswerId(),
                                new GradingClient.RetryRequest("自动重试", answer.getAnswerId() + "-auto-" + attempt));
                        if (response.data() == null || "MANUAL_REVIEW".equals(response.data().status())) break;
                    }
                } catch (Exception ex) { log.warn("答卷重试暂不可用 {}", answer.getAnswerId(), ex); break; }
            }
            if (tryProcess(batch, rubric, answer)) return;
        }
        try {
            var state = gradingClient.get("TASK-" + answer.getAnswerId()).data();
            if (state == null || !"MANUAL_REVIEW".equals(state.status())) {
                answer.setStatus("FAILED"); answerRepository.save(answer); return;
            }
        } catch (Exception ex) { answer.setStatus("FAILED"); answerRepository.save(answer); return; }
        deliverManualReview(batch, rubric, answer);
    }

    private void deliverManualReview(Batch batch, RubricClient.RemoteRubric rubric, Answer answer) {
        try { reviewClient.create(new ReviewClient.CreateResultRequest(answer.getAnswerId(), batch.getBatchId(),
                answer.getStudentNo(), answer.getOcrText() == null ? "" : answer.getOcrText(), batch.getTeacherId(),
                rubric.rubricVersion(), BigDecimal.ZERO, batch.getMaxScore(), answer.getOcrCoverageRatio(),
                BigDecimal.ZERO, List.of(), "MANUAL_REVIEW", scoringPoints(rubric), batch.getBaseScore()));
            answer.setStatus("MANUAL_REVIEW"); answerRepository.save(answer);
            safeNotify(batch, "grading.failed", "答卷需人工处理", "答卷 " + answer.getStudentNo() + " 初筛失败，已进入人工复核清单。");
        } catch (Exception ex) { answer.setStatus("FAILED"); answerRepository.save(answer); log.warn("人工复核投递失败 {}", answer.getAnswerId(), ex); }
    }

    private List<ReviewClient.ScoringPoint> scoringPoints(RubricClient.RemoteRubric rubric) {
        return rubric.points().stream().map(p -> new ReviewClient.ScoringPoint(p.pointId(), p.pointName())).toList();
    }

    private boolean tryProcess(Batch batch, RubricClient.RemoteRubric rubric, Answer answer) {
        try {
            answer.setStatus("RUNNING");
            answerRepository.save(answer);
            GradingClient.OcrResult ocr = gradingClient.ocr("TASK-" + answer.getAnswerId(),
                    new GradingClient.OcrRequest(answer.getAnswerId(), answer.getObjectKey(),
                            "TEXT".equals(answer.getSourceType()) ? answer.getOcrText() : null)).data();
            if (ocr == null) throw BusinessException.unavailable("识别服务暂时不可用");
            if (ocr.failureReason() != null && !ocr.failureReason().isBlank()) {
                throw new IllegalStateException(ocr.failureReason());
            }
            answer.setOcrText(ocr.text());
            answer.setOcrConfidence(ocr.confidence());
            answer.setOcrCoverageRatio(ocr.answerCoverageRatio());
            answer.setOcrCharacterCount(ocr.characterCount());
            answerRepository.save(answer);
            GradingClient.MatchResult match = gradingClient.match("TASK-" + answer.getAnswerId(),
                    new GradingClient.MatchRequest(ocr.text(), rubric.rubricVersion(), rubric.points(),
                            ocr.answerCoverageRatio(), batch.getMaxScore(), batch.getBaseScore())).data();
            if (match == null) {
                throw BusinessException.unavailable("评分服务暂时不可用，已触发服务降级");
            }
            reviewClient.create(new ReviewClient.CreateResultRequest(
                    answer.getAnswerId(), batch.getBatchId(), answer.getStudentNo(), ocr.text(),
                    batch.getTeacherId(), rubric.rubricVersion(), match.suggestedScore(), batch.getMaxScore(),
                    ocr.answerCoverageRatio(),
                    match.confidence(), match.hits(), "WAITING_REVIEW", scoringPoints(rubric), batch.getBaseScore()));
            answer.setStatus("WAITING_REVIEW");
            answerRepository.save(answer);
            return true;
        } catch (Exception ex) {
            log.warn("答卷 {} 初筛失败", answer.getAnswerId(), ex);
            answer.setStatus("FAILED");
        }
        answerRepository.save(answer);
        return false;
    }

    private void safeNotify(Batch batch, String type, String title, String content) {
        try {
            eventPublisher.publish(new DomainEvent(java.util.UUID.randomUUID().toString(), type,
                    batch.getTeacherId(), batch.getBatchId(), title, content));
        } catch (Exception ex) {
            log.warn("通知发送失败，不影响主流程", ex);
        }
    }

    public GradingClient.TaskView retry(Long teacherId, String taskId, String reason, String key) {
        if (key == null || !key.matches("[A-Za-z0-9_-]{8,64}")) throw BusinessException.badRequest("Idempotency-Key 须为8至64位");
        if (!taskId.startsWith("TASK-")) throw BusinessException.notFound("初筛任务不存在");
        Answer answer = answerRepository.findById(taskId.substring(5)).orElseThrow(() -> BusinessException.notFound("答卷不存在"));
        Batch batch = batchRepository.findById(answer.getBatchId()).orElseThrow();
        if (!teacherId.equals(batch.getTeacherId())) throw BusinessException.forbidden("不能重试其他教师的答卷");
        String guard = "manual-retry:" + taskId + ":" + key;
        var cached = guards.get(guard);
        if (cached.isPresent()) return readRetry(cached.get());
        String lock = "retry-running:" + taskId;
        if (!guards.putIfAbsent(lock, key, java.time.Duration.ofMinutes(10))) throw BusinessException.badRequest("任务正在重试，请稍后读取进度");
        try {
            if (!List.of("FAILED", "MANUAL_REVIEW").contains(answer.getStatus())) throw BusinessException.badRequest("仅失败任务可重试");
            GradingClient.TaskView state;
            try { state = gradingClient.get(taskId).data(); }
            catch (feign.FeignException.NotFound ex) { state = new GradingClient.TaskView(taskId, answer.getAnswerId(), "PENDING", 0, null); }
            var task = state != null && List.of("PENDING", "WAITING_REVIEW", "MANUAL_REVIEW").contains(state.status())
                    ? state : gradingClient.retry(taskId, new GradingClient.RetryRequest(reason, key)).data();
            if (task == null) throw BusinessException.unavailable("初筛服务暂时不可用");
            var rubric = boundRubric(jobRepository.findById(answer.getImportJobId()).orElseThrow());
            if (rubric == null) throw BusinessException.unavailable("评分规则服务暂时不可用");
            if (!"MANUAL_REVIEW".equals(task.status())) {
                tryProcess(batch, rubric, answer);
            } else { deliverManualReview(batch, rubric, answer); }
            var result = new GradingClient.TaskView(taskId, answer.getAnswerId(), answer.getStatus(), task.retryCount(), task.errorCode());
            guards.putIfAbsent(guard, new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(result), java.time.Duration.ofDays(1));
            return result;
        } catch (com.fasterxml.jackson.core.JsonProcessingException ex) { throw new IllegalStateException(ex); }
        finally { guards.delete(lock); }
    }

    private GradingClient.TaskView readRetry(String json) {
        try { return new com.fasterxml.jackson.databind.ObjectMapper().readValue(json, GradingClient.TaskView.class); }
        catch (Exception ex) { throw new IllegalStateException("重试记录损坏", ex); }
    }

    private RubricClient.RemoteRubric boundRubric(ImportJob job) {
        if (job.getRubricSnapshotJson() == null) return rubricClient.current(job.getBatchId()).data();
        try { return new com.fasterxml.jackson.databind.ObjectMapper().readValue(job.getRubricSnapshotJson(), RubricClient.RemoteRubric.class); }
        catch (Exception ex) { throw new IllegalStateException("导入任务规则快照损坏", ex); }
    }
}
