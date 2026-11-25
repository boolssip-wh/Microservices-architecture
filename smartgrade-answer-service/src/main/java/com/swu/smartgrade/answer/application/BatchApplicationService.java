package com.swu.smartgrade.answer.application;

import com.swu.smartgrade.answer.api.dto.AnswerView;
import com.swu.smartgrade.answer.api.dto.BatchDetailView;
import com.swu.smartgrade.answer.api.dto.BatchView;
import com.swu.smartgrade.answer.api.dto.CreateBatchRequest;
import com.swu.smartgrade.answer.api.dto.ProgressView;
import com.swu.smartgrade.answer.domain.Answer;
import com.swu.smartgrade.answer.domain.AnswerRepository;
import com.swu.smartgrade.answer.domain.Batch;
import com.swu.smartgrade.answer.domain.BatchRepository;
import com.swu.smartgrade.common.api.BusinessException;
import com.swu.smartgrade.common.api.PageResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
public class BatchApplicationService {
    private final BatchRepository batchRepository;
    private final AnswerRepository answerRepository;
    private final com.swu.smartgrade.answer.infrastructure.client.RubricClient rubrics;
    private final com.swu.smartgrade.answer.infrastructure.client.ReviewClient reviews;

    public BatchApplicationService(BatchRepository batchRepository, AnswerRepository answerRepository,
            com.swu.smartgrade.answer.infrastructure.client.RubricClient rubrics,
            com.swu.smartgrade.answer.infrastructure.client.ReviewClient reviews) {
        this.batchRepository = batchRepository;
        this.answerRepository = answerRepository;
        this.rubrics = rubrics;
        this.reviews = reviews;
    }

    @Transactional
    public BatchView create(Long teacherId, CreateBatchRequest request) {
        BigDecimal baseScore = com.swu.smartgrade.common.util.ScorePolicy.baseScore(request.maxScore(), request.baseScore());
        Batch batch = new Batch();
        batch.setBatchId(id("B"));
        batch.setTeacherId(teacherId);
        batch.setExamName(request.examName());
        batch.setQuestionNo(request.questionNo());
        batch.setQuestionTitle(request.questionTitle());
        batch.setQuestionText(request.questionText());
        batch.setMaxScore(request.maxScore());
        batch.setBaseScore(baseScore);
        batch.setStatus("DRAFT");
        batch.setCreatedAt(LocalDateTime.now());
        batchRepository.save(batch);
        return view(batch);
    }

    @Transactional
    public PageResult<BatchView> list(Long teacherId, String status, int page, int size) {
        PageRequest pageable = PageRequest.of(Math.max(0, page - 1), Math.min(100, Math.max(1, size)),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Batch> result = status == null || status.isBlank()
                ? batchRepository.findByTeacherId(teacherId, pageable)
                : batchRepository.findByTeacherIdAndStatus(teacherId, status, pageable);
        result.getContent().forEach(batch -> reconcile(batch.getBatchId()));
        return PageResult.of(result.getContent().stream().map(this::view).toList(),
                result.getTotalElements(), page, result.getSize());
    }

    @Transactional
    public BatchDetailView detail(Long teacherId, String batchId) {
        Batch batch = requireOwned(teacherId, batchId);
        Integer version = null;
        try { var current = rubrics.current(batchId).data(); if (current != null && current.rubricVersion() > 0) version = current.rubricVersion(); }
        catch (feign.FeignException.NotFound ex) { /* A draft batch does not have a frozen rule yet. */ }
        var progress = progress(batchId);
        return new BatchDetailView(batch.getBatchId(), batch.getExamName(), batch.getQuestionNo(),
                batch.getQuestionTitle(), batch.getQuestionText(), batch.getMaxScore(), batch.getStatus(),
                offset(batch), progress, view(batch), version,
                com.swu.smartgrade.common.util.ScorePolicy.baseScore(batch.getMaxScore(), batch.getBaseScore()));
    }

    @Transactional
    public ProgressView progress(String batchId) {
        reconcile(batchId);
        return new ProgressView(
                answerRepository.countByBatchId(batchId),
                answerRepository.countByBatchIdAndStatus(batchId, "PENDING"),
                answerRepository.countByBatchIdAndStatus(batchId, "RUNNING"),
                answerRepository.countByBatchIdAndStatus(batchId, "WAITING_REVIEW"),
                answerRepository.countByBatchIdAndStatus(batchId, "MANUAL_REVIEW"),
                answerRepository.countByBatchIdAndStatus(batchId, "FAILED")
        );
    }

    @Transactional
    public PageResult<AnswerView> answers(Long teacherId, String batchId, String status, int page, int size) {
        requireOwned(teacherId, batchId);
        reconcile(batchId);
        List<Answer> answers = answerRepository.findByBatchIdOrderByStudentNo(batchId).stream()
                .filter(answer -> status == null || status.isBlank() || status.equals(answer.getStatus()))
                .toList();
        int safePage = Math.max(1, page);
        int safeSize = Math.min(100, Math.max(1, size));
        int from = Math.min(answers.size(), (safePage - 1) * safeSize);
        int to = Math.min(answers.size(), from + safeSize);
        return PageResult.of(answers.subList(from, to).stream().map(this::answerView).toList(),
                answers.size(), safePage, safeSize);
    }

    public Batch requireOwned(Long teacherId, String batchId) {
        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> BusinessException.notFound("批次不存在"));
        if (!teacherId.equals(batch.getTeacherId())) {
            throw BusinessException.forbidden("不能访问其他教师的批次");
        }
        return batch;
    }

    private BatchView view(Batch batch) {
        return new BatchView(batch.getBatchId(), batch.getExamName(), batch.getQuestionNo(),
                batch.getQuestionTitle(), batch.getMaxScore(), batch.getStatus(), offset(batch),
                answerRepository.countByBatchId(batch.getBatchId()),
                com.swu.smartgrade.common.util.ScorePolicy.baseScore(batch.getMaxScore(), batch.getBaseScore()));
    }

    private AnswerView answerView(Answer answer) {
        return new AnswerView(answer.getAnswerId(), answer.getStudentNo(), answer.getSourceType(),
                answer.getStatus(), answer.getOcrConfidence(), answer.getOcrCoverageRatio(),
                answer.getOcrCharacterCount(),
                answer.getCreatedAt().atZone(ZoneId.systemDefault()).toOffsetDateTime(), "TASK-" + answer.getAnswerId());
    }

    private java.time.OffsetDateTime offset(Batch batch) {
        return batch.getCreatedAt().atZone(ZoneId.systemDefault()).toOffsetDateTime();
    }

    private String id(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }

    private void reconcile(String batchId) {
        var localAnswers = answerRepository.findByBatchIdOrderByStudentNo(batchId);
        if (localAnswers.isEmpty()) return;
        try {
            var states = reviews.states(batchId).data();
            if (states == null) return;
            var byId = states.stream().collect(java.util.stream.Collectors.toMap(
                    com.swu.smartgrade.answer.infrastructure.client.ReviewClient.ReviewState::answerId,
                    com.swu.smartgrade.answer.infrastructure.client.ReviewClient.ReviewState::status));
            for (Answer answer : localAnswers) {
                String status = byId.get(answer.getAnswerId());
                if (status != null && !status.equals(answer.getStatus())) { answer.setStatus(status); answerRepository.save(answer); }
            }
            if (localAnswers.stream().allMatch(a -> "REVIEWED".equals(a.getStatus()))) {
                var batch = batchRepository.findById(batchId).orElseThrow();
                batch.setStatus("COMPLETED"); batchRepository.save(batch);
            }
        } catch (feign.FeignException ex) {
            org.slf4j.LoggerFactory.getLogger(getClass()).warn("复核状态同步暂不可用，保留最近进度 {}", batchId);
        }
    }
}
