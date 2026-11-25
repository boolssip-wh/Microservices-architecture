package com.swu.smartgrade.review.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swu.smartgrade.common.api.BusinessException;
import com.swu.smartgrade.common.api.PageResult;
import com.swu.smartgrade.review.api.dto.*;
import com.swu.smartgrade.review.domain.*;
import com.swu.smartgrade.common.event.DomainEvent;
import com.swu.smartgrade.common.event.DomainEventPublisher;
import com.swu.smartgrade.review.infrastructure.client.UserClient;
import com.swu.smartgrade.review.infrastructure.ai.CommentAiClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ReviewApplicationService {
    private final GradingResultRepository resultRepository;
    private final HitEvidenceRepository evidenceRepository;
    private final ReviewRecordRepository reviewRepository;
    private final ScoreSnapshotRepository snapshotRepository;
    private final CommentDraftRepository commentRepository;
    private final AuditLogRepository auditRepository;
    private final UserClient userClient;
    private final CommentAiClient commentAiClient;
    private final DomainEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    public ReviewApplicationService(GradingResultRepository resultRepository,
                                    HitEvidenceRepository evidenceRepository,
                                    ReviewRecordRepository reviewRepository,
                                    ScoreSnapshotRepository snapshotRepository,
                                    CommentDraftRepository commentRepository,
                                    AuditLogRepository auditRepository,
                                    UserClient userClient,
                                    CommentAiClient commentAiClient,
                                    DomainEventPublisher eventPublisher,
                                    ObjectMapper objectMapper) {
        this.resultRepository = resultRepository;
        this.evidenceRepository = evidenceRepository;
        this.reviewRepository = reviewRepository;
        this.snapshotRepository = snapshotRepository;
        this.commentRepository = commentRepository;
        this.auditRepository = auditRepository;
        this.userClient = userClient;
        this.commentAiClient = commentAiClient;
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public CreatedResult create(CreateResultRequest request) {
        GradingResult result = resultRepository.findByAnswerId(request.answerId()).orElseGet(GradingResult::new);
        if (result.getFinalScore() != null || "REVIEWED".equals(result.getStatus())) {
            return new CreatedResult(result.getResultId(), result.getStatus());
        }
        if (result.getResultId() == null) {
            result.setResultId(id("S"));
            result.setAnswerId(request.answerId());
            result.setCreatedAt(LocalDateTime.now());
        }
        result.setBatchId(request.batchId());
        result.setStudentNo(request.studentNo());
        result.setAnswerText(request.answerText());
        result.setTeacherId(request.teacherId());
        result.setRubricVersion(request.rubricVersion());
        try {
            result.setScoringPointsJson(objectMapper.writeValueAsString(
                    request.scoringPoints() == null ? List.of() : request.scoringPoints()));
        } catch (Exception ex) {
            throw BusinessException.badRequest("评分要点目录格式错误");
        }
        result.setSuggestedScore(request.suggestedScore());
        result.setMaxScore(request.maxScore());
        result.setBaseScore(com.swu.smartgrade.common.util.ScorePolicy.baseScore(request.maxScore(), request.baseScore()));
        result.setAnswerCoverageRatio(request.answerCoverageRatio());
        result.setConfidence(request.confidence());
        result.setStatus("MANUAL_REVIEW".equals(request.status()) ? "MANUAL_REVIEW" : "WAITING_REVIEW");
        resultRepository.save(result);
        evidenceRepository.deleteAll(evidenceRepository.findByResultId(result.getResultId()));
        for (HitInput hit : request.hits()) {
            HitEvidence evidence = new HitEvidence();
            evidence.setEvidenceId(id("E"));
            evidence.setResultId(result.getResultId());
            evidence.setPointId(hit.pointId());
            evidence.setPointName(hit.pointName());
            evidence.setMatchType(hit.matchType());
            evidence.setSentence(hit.evidence());
            evidence.setStartOffset(hit.start());
            evidence.setEndOffset(hit.end());
            evidence.setConfidence(hit.confidence());
            evidenceRepository.save(evidence);
        }
        return new CreatedResult(result.getResultId(), result.getStatus());
    }

    @Transactional(readOnly = true)
    public PageResult<ResultListItem> list(Long teacherId, String batchId, String status, int page, int size) {
        List<ResultListItem> items = resultRepository.findByTeacherIdOrderByCreatedAtDesc(teacherId).stream()
                .filter(item -> batchId == null || batchId.isBlank() || batchId.equals(item.getBatchId()))
                .filter(item -> status == null || status.isBlank() || status.equals(item.getStatus()))
                .map(item -> new ResultListItem(item.getAnswerId(), item.getStudentNo(),
                        item.getSuggestedScore(), item.getFinalScore(), item.getMaxScore(),
                        item.getConfidence(), item.getStatus()))
                .toList();
        int safePage = Math.max(1, page);
        int safeSize = Math.min(100, Math.max(1, size));
        int from = Math.min(items.size(), (safePage - 1) * safeSize);
        int to = Math.min(items.size(), from + safeSize);
        return PageResult.of(items.subList(from, to), items.size(), safePage, safeSize);
    }

    @Transactional(readOnly = true)
    public ResultDetail detail(Long teacherId, String answerId) {
        GradingResult result = requireResult(teacherId, answerId);
        ReviewRecord review = reviewRepository.findByResultId(result.getResultId()).orElse(null);
        CommentDraft draft = review == null ? null : commentRepository.findByReviewId(review.getReviewId()).orElse(null);
        List<EvidenceView> evidence = evidenceRepository.findByResultId(result.getResultId()).stream()
                .map(item -> new EvidenceView(item.getEvidenceId(), item.getPointId(), item.getPointName(),
                        item.getMatchType(), item.getSentence(), item.getStartOffset(), item.getEndOffset(),
                        item.getConfidence()))
                .toList();
        CommentView comment = draft == null ? null
                : new CommentView(draft.getSuggestedText(), draft.getFinalText(), draft.getStatus(),
                draft.getProfileVersion());
        return new ResultDetail(result.getResultId(), result.getAnswerId(), result.getStudentNo(),
                result.getAnswerText(), result.getRubricVersion(), result.getConfidence(),
                result.getSuggestedScore(), result.getFinalScore(), result.getMaxScore(),
                result.getAnswerCoverageRatio(),
                result.getStatus(), evidence, comment,
                com.swu.smartgrade.common.util.ScorePolicy.baseScore(result.getMaxScore(), result.getBaseScore()));
    }

    @Transactional
    public CommentSuggestionResponse suggestComment(Long teacherId, String answerId,
                                                    CommentSuggestionRequest request) {
        GradingResult result = requireResult(teacherId, answerId);
        UserClient.CommentProfile profile = loadProfile(teacherId);
        List<HitEvidence> evidence = evidenceRepository.findByResultId(result.getResultId());
        String hits = evidence.isEmpty() ? "暂未识别到明确命中要点"
                : String.join("、", evidence.stream().map(HitEvidence::getPointName).toList());
        String evidenceContext = "本题最终分：" + request.finalScore() + "/" + result.getMaxScore()
                + "；命中要点：" + hits + "；"
                + (evidence.isEmpty() ? "无明确证据。"
                : "证据：" + String.join("；", evidence.stream()
                .map(item -> item.getPointName() + "：“" + item.getSentence() + "”")
                .toList()))
                + "；教师偏好：" + profile.styleTag() + "；篇幅偏好：" + profile.lengthMode() + "。";
        List<String> generated = commentAiClient.generate(evidenceContext, request.teacherInstruction())
                .orElse(null);
        String styleFirst;
        String styleSecond;
        String generationMode;
        if (generated != null) {
            styleFirst = generated.get(0);
            styleSecond = generated.get(1);
            generationMode = "AI";
        } else {
            String opening = switch (profile.openingStyle()) {
                case "DIRECT" -> "根据本次作答";
                case "QUESTION" -> "你是否还能补充遗漏的关键要点";
                case "PRAISE_FIRST" -> "已看到你在相关要点上的努力";
                default -> profile.openingStyle();
            };
            String encouragement = switch (profile.tone()) {
                case "ENCOURAGING" -> "继续保持，逐项补充会更完整。";
                case "STRICT" -> "请严格按评分要点补全依据。";
                default -> "请结合原文证据核对遗漏。";
            };
            String first = opening + "。本题得分 " + request.finalScore() + "/" + result.getMaxScore()
                    + "，已识别要点：" + hits + "。建议结合证据继续完善关键步骤。";
            String second = "从现有作答看，" + (evidence.isEmpty() ? "需要重新核对题意与评分要点"
                    : "你已经覆盖了 " + hits + "；可再检查遗漏要点和表达准确性") + "。";
            first += encouragement;
            second += encouragement;
            if ("SHORT".equals(profile.lengthMode())) {
                first = opening + "。已识别：" + hits + "。" + encouragement;
                second = "本题 " + request.finalScore() + "/" + result.getMaxScore() + "。" + encouragement;
            } else if ("LONG".equals(profile.lengthMode())) {
                first += "建议逐句比对参考要点，补充概念之间的联系，并用具体例子说明推理过程。";
                second += "完成修改后，请再次核对题意、证据与结论是否一致。";
            }
            styleFirst = applyBans(first, profile.bannedPhrases());
            styleSecond = second;
            if (!profile.commonPhrases().isEmpty()) {
                styleSecond = styleSecond + " " + profile.commonPhrases().get(0);
            }
            styleSecond = applyBans(styleSecond, profile.bannedPhrases());
            generationMode = "TEMPLATE";
        }
        ReviewRecord review = reviewRepository.findByResultId(result.getResultId()).orElseGet(ReviewRecord::new);
        if (review.getReviewId() == null) {
            review.setReviewId(id("RV"));
            review.setResultId(result.getResultId());
            review.setTeacherId(teacherId);
            review.setReviewStatus(result.getStatus());
            reviewRepository.save(review);
        }
        CommentDraft draft = commentRepository.findByReviewId(review.getReviewId()).orElseGet(CommentDraft::new);
        if (draft.getCommentId() == null) {
            draft.setCommentId(id("C"));
            draft.setReviewId(review.getReviewId());
            draft.setCreatedAt(LocalDateTime.now());
        }
        draft.setTeacherId(teacherId);
        draft.setProfileId(profile.profileId());
        draft.setProfileVersion(profile.profileVersion());
        draft.setSuggestedText(styleFirst + "\n\n" + styleSecond);
        if (!"CONFIRMED".equals(draft.getStatus())) {
            draft.setStatus("DRAFT");
        }
        commentRepository.save(draft);
        return new CommentSuggestionResponse(List.of(styleFirst, styleSecond),
                evidence.stream().map(HitEvidence::getEvidenceId).toList(),
                profile.styleTag(), profile.profileVersion(), generationMode);
    }

    @Transactional
    public FinalScoreResponse finalizeScore(Long teacherId, String answerId, FinalScoreRequest request) {
        GradingResult result = requireResult(teacherId, answerId);
        if (request.finalScore().compareTo(BigDecimal.ZERO) < 0
                || request.finalScore().compareTo(result.getMaxScore()) > 0) {
            throw BusinessException.badRequest("最终分必须在 0 到 " + result.getMaxScore() + " 之间");
        }
        BigDecimal finalScore = request.finalScore().setScale(2, RoundingMode.HALF_UP);
        ReviewRecord review = reviewRepository.findByResultId(result.getResultId()).orElseGet(ReviewRecord::new);
        if (review.getReviewId() == null) {
            review.setReviewId(id("RV"));
            review.setResultId(result.getResultId());
        }
        review.setTeacherId(teacherId);
        review.setFinalScore(finalScore);
        review.setReviewStatus("REVIEWED");
        review.setReviewNote(request.reviewNote());
        review.setConfirmedAt(LocalDateTime.now());
        reviewRepository.save(review);

        result.setFinalScore(finalScore);
        result.setStatus("REVIEWED");
        result.setReviewedAt(LocalDateTime.now());
        resultRepository.save(result);

        CommentDraft draft = commentRepository.findByReviewId(review.getReviewId()).orElseGet(CommentDraft::new);
        if (draft.getCommentId() == null) {
            draft.setCommentId(id("C"));
            draft.setReviewId(review.getReviewId());
            draft.setCreatedAt(LocalDateTime.now());
        }
        draft.setTeacherId(teacherId);
        draft.setProfileVersion(request.commentProfileVersion());
        draft.setFinalText(request.finalComment());
        draft.setStatus("CONFIRMED");
        commentRepository.save(draft);

        ScoreSnapshot snapshot = new ScoreSnapshot();
        snapshot.setSnapshotId(id("SS"));
        snapshot.setReviewId(review.getReviewId());
        snapshot.setFinalScore(finalScore);
        snapshot.setSnapshotJson(writeSnapshot(result, review, draft));
        snapshot.setCreatedAt(LocalDateTime.now());
        snapshotRepository.save(snapshot);

        AuditLog audit = new AuditLog();
        audit.setAuditId(id("AU"));
        audit.setActorId(teacherId);
        audit.setAction("FINALIZE_SCORE");
        audit.setObjectType("RESULT");
        audit.setObjectId(result.getResultId());
        audit.setSummaryJson("{\"answerId\":\"" + answerId + "\",\"finalScore\":" + finalScore + "}");
        audit.setCreatedAt(LocalDateTime.now());
        auditRepository.save(audit);
        safeNotify(result, "复核完成", "答卷 " + result.getStudentNo() + " 已确认最终分："
                + finalScore + "/" + result.getMaxScore());
        return new FinalScoreResponse(review.getReviewId(), finalScore,
                snapshot.getSnapshotId(), audit.getAuditId());
    }

    @Transactional
    public ReviewStatusResponse updateStatus(Long teacherId, String answerId, ReviewStatusRequest request) {
        GradingResult result = requireResult(teacherId, answerId);
        String status = switch (request.status()) {
            case "WAITING" -> "WAITING_REVIEW";
            case "MANUAL" -> "MANUAL_REVIEW";
            default -> request.status();
        };
        if (!List.of("WAITING_REVIEW", "MANUAL_REVIEW", "REVIEWED").contains(status)) {
            throw BusinessException.badRequest("状态只允许 WAITING_REVIEW、MANUAL_REVIEW 或 REVIEWED");
        }
        if ("REVIEWED".equals(status) && result.getFinalScore() == null) {
            throw BusinessException.conflict("请先确认最终分与评语");
        }
        if (result.getFinalScore() != null && !"REVIEWED".equals(status)) {
            throw BusinessException.conflict("已定分答卷请通过最终定分接口修订，不能退回待复核状态");
        }
        ReviewRecord review = reviewRepository.findByResultId(result.getResultId()).orElseGet(ReviewRecord::new);
        if (review.getReviewId() == null) {
            review.setReviewId(id("RV"));
            review.setResultId(result.getResultId());
            review.setTeacherId(teacherId);
        }
        review.setReviewStatus(status);
        review.setReviewNote(request.reason());
        reviewRepository.save(review);
        result.setStatus(status);
        resultRepository.save(result);
        return new ReviewStatusResponse(review.getReviewId(), review.getReviewStatus(),
                LocalDateTime.now().atZone(ZoneId.systemDefault()).toOffsetDateTime());
    }

    @Transactional(readOnly = true)
    public ResultSummary summary(String batchId) {
        List<GradingResult> results = resultRepository.findByBatchId(batchId);
        List<GradingResult> reviewed = results.stream()
                .filter(item -> item.getFinalScore() != null)
                .toList();
        List<ResultSummary.FinalizedScore> scores = reviewed.stream()
                .map(item -> new ResultSummary.FinalizedScore(item.getAnswerId(), item.getStudentNo(),
                        item.getFinalScore(), item.getMaxScore()))
                .toList();
        Map<String, String> pointNames = new LinkedHashMap<>();
        for (GradingResult result : reviewed) {
            if (result.getScoringPointsJson() != null) {
                try {
                    var catalog = objectMapper.readValue(result.getScoringPointsJson(),
                            CreateResultRequest.ScoringPoint[].class);
                    for (var point : catalog) pointNames.putIfAbsent(point.pointId(), point.pointName());
                } catch (Exception ex) {
                    throw new IllegalStateException("评分要点目录无法读取", ex);
                }
            }
            List<HitEvidence> evidence = evidenceRepository.findByResultId(result.getResultId());
            for (HitEvidence hit : evidence) {
                pointNames.putIfAbsent(hit.getPointId(), hit.getPointName());
            }
        }
        long denominator = Math.max(1, reviewed.size());
        List<ResultSummary.PointMissCount> counts = pointNames.entrySet().stream()
                .map(entry -> {
                    long misses = reviewed.stream()
                            .filter(result -> evidenceRepository.findByResultId(result.getResultId()).stream()
                                    .noneMatch(hit -> entry.getKey().equals(hit.getPointId())))
                            .count();
                    return new ResultSummary.PointMissCount(entry.getKey(), entry.getValue(), misses);
                })
                .toList();
        List<ResultSummary.PointMissRate> rates = counts.stream()
                .map(item -> new ResultSummary.PointMissRate(item.pointId(), item.pointName(),
                        (double) item.missCount() / denominator))
                .toList();
        return new ResultSummary(scores, counts, rates);
    }

    @Transactional(readOnly = true)
    public List<ResultState> states(Long teacherId, String batchId) {
        return ownedBatch(teacherId, batchId).stream()
                .map(result -> new ResultState(result.getAnswerId(), result.getStatus())).toList();
    }

    @Transactional(readOnly = true)
    public ResultSummary ownedSummary(Long teacherId, String batchId) {
        ownedBatch(teacherId, batchId);
        return summary(batchId);
    }

    private List<GradingResult> ownedBatch(Long teacherId, String batchId) {
        List<GradingResult> results = resultRepository.findByBatchId(batchId);
        if (results.stream().anyMatch(result -> !teacherId.equals(result.getTeacherId()))) {
            throw BusinessException.forbidden("不能访问其他教师的批次");
        }
        return results;
    }

    public record ResultState(String answerId, String status) { }

    private GradingResult requireResult(Long teacherId, String answerId) {
        GradingResult result = resultRepository.findByAnswerId(answerId)
                .orElseThrow(() -> BusinessException.notFound("初筛结果不存在"));
        if (!teacherId.equals(result.getTeacherId())) {
            throw BusinessException.forbidden("不能复核其他教师的批次");
        }
        return result;
    }

    private UserClient.CommentProfile loadProfile(Long teacherId) {
        try {
            UserClient.CommentProfile profile = userClient.profile(teacherId, "teacher", "教师", "TEACHER").data();
            if (profile == null) {
                return fallbackProfile(teacherId);
            }
            return profile;
        } catch (Exception ex) {
            return fallbackProfile(teacherId);
        }
    }

    private UserClient.CommentProfile fallbackProfile(Long teacherId) {
        return new UserClient.CommentProfile(null, teacherId, "默认模板", "NEUTRAL",
                "MEDIUM", "PRAISE_FIRST", List.of(), List.of(), 0);
    }

    private String applyBans(String text, List<String> bannedPhrases) {
        String value = text;
        for (String banned : bannedPhrases) {
            if (banned != null && !banned.isBlank()) {
                value = value.replace(banned, "");
            }
        }
        return value;
    }

    private void safeNotify(GradingResult result, String title, String content) {
        try {
            eventPublisher.publish(new DomainEvent("review-" + UUID.randomUUID(), "review.confirmed",
                    result.getTeacherId(), result.getBatchId(), title, content));
        } catch (Exception ignored) {
            // 通知失败不回滚教师已经确认的分数。
        }
    }

    private String writeSnapshot(GradingResult result, ReviewRecord review, CommentDraft draft) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "answerId", result.getAnswerId(),
                    "suggestedScore", result.getSuggestedScore(),
                    "finalScore", review.getFinalScore(),
                    "finalComment", draft.getFinalText()
            ));
        } catch (Exception ex) {
            throw new IllegalStateException("成绩快照序列化失败", ex);
        }
    }

    private String id(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }
}
