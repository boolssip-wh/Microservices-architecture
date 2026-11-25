package com.swu.smartgrade.rubric.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swu.smartgrade.common.api.BusinessException;
import com.swu.smartgrade.rubric.api.dto.CompileRubricResponse;
import com.swu.smartgrade.rubric.api.dto.CreateRubricRequest;
import com.swu.smartgrade.rubric.api.dto.CreateRubricResponse;
import com.swu.smartgrade.rubric.api.dto.ParseRubricResponse;
import com.swu.smartgrade.rubric.api.dto.RubricPointRequest;
import com.swu.smartgrade.rubric.api.dto.RubricPointView;
import com.swu.smartgrade.rubric.api.dto.RubricVersionView;
import com.swu.smartgrade.rubric.api.dto.UpdatePointsResponse;
import com.swu.smartgrade.rubric.domain.PointVariant;
import com.swu.smartgrade.rubric.domain.PointVariantRepository;
import com.swu.smartgrade.rubric.domain.Rubric;
import com.swu.smartgrade.rubric.domain.RubricPoint;
import com.swu.smartgrade.rubric.domain.RubricPointRepository;
import com.swu.smartgrade.rubric.domain.RubricRepository;
import com.swu.smartgrade.rubric.domain.RubricVersionRepository;
import com.swu.smartgrade.rubric.domain.RubricVersionSnapshot;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class RubricApplicationService {
    private final RubricRepository rubricRepository;
    private final RubricPointRepository pointRepository;
    private final PointVariantRepository variantRepository;
    private final RubricVersionRepository versionRepository;
    private final ObjectMapper objectMapper;

    public RubricApplicationService(RubricRepository rubricRepository,
                                    RubricPointRepository pointRepository,
                                    PointVariantRepository variantRepository,
                                    RubricVersionRepository versionRepository,
                                    ObjectMapper objectMapper) {
        this.rubricRepository = rubricRepository;
        this.pointRepository = pointRepository;
        this.variantRepository = variantRepository;
        this.versionRepository = versionRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public CreateRubricResponse create(CreateRubricRequest request) {
        Rubric rubric = new Rubric();
        rubric.setRubricId(id("R"));
        rubric.setBatchId(request.batchId());
        rubric.setSourceText(request.sourceText());
        rubric.setMaxScore(request.maxScore());
        rubric.setBaseScore(com.swu.smartgrade.common.util.ScorePolicy.baseScore(request.maxScore(), request.baseScore()));
        rubric.setVersion(0);
        rubric.setStatus("DRAFT");
        rubric.setCreatedAt(LocalDateTime.now());
        rubricRepository.save(rubric);
        return new CreateRubricResponse(rubric.getRubricId(), 0, "DRAFT");
    }

    @Transactional
    public ParseRubricResponse parse(String rubricId, String sourceText) {
        Rubric rubric = find(rubricId);
        requireDraft(rubric);
        rubric.setSourceText(sourceText == null || sourceText.isBlank() ? rubric.getSourceText() : sourceText);
        rubricRepository.save(rubric);
        List<String> lines = java.util.Arrays.stream(rubric.getSourceText().split("[\\n;；]+"))
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .toList();
        if (lines.isEmpty() || lines.size() > 50) {
            throw BusinessException.badRequest("评分细则须解析为1至50个要点");
        }
        BigDecimal equalWeight = BigDecimal.valueOf(100)
                .divide(BigDecimal.valueOf(lines.size()), 2, RoundingMode.HALF_UP);
        BigDecimal available = availableScore(rubric);
        BigDecimal equalScore = available
                .divide(BigDecimal.valueOf(lines.size()), 2, RoundingMode.DOWN);
        BigDecimal assignedWeight = BigDecimal.ZERO;
        BigDecimal assignedScore = BigDecimal.ZERO;
        List<RubricPointView> points = new ArrayList<>();
        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index);
            String[] pieces = line.split("[:：]");
            String name = pieces[0].replaceFirst("^\\d+[.、\\s]+", "").trim();
            List<String> keywords = pieces.length > 1
                    ? java.util.Arrays.stream(pieces[1].split("[|,，、]")).map(String::trim)
                    .filter(item -> !item.isBlank()).toList()
                    : List.of(name);
            boolean lengthPoint = name.contains("篇幅")
                    || keywords.stream().anyMatch("LENGTH_COVERAGE"::equalsIgnoreCase);
            String pointType = lengthPoint ? "LENGTH_COVERAGE" : classify(name);
            if (lengthPoint) {
                keywords = List.of();
            }
            boolean last = index == lines.size() - 1;
            BigDecimal weight = last
                    ? BigDecimal.valueOf(100).subtract(assignedWeight)
                    : equalWeight;
            BigDecimal score = last
                    ? available.subtract(assignedScore)
                    : equalScore;
            points.add(new RubricPointView("P" + (index + 1), "P" + (index + 1), name,
                    pointType, keywords, List.of(), weight, score));
            assignedWeight = assignedWeight.add(weight);
            assignedScore = assignedScore.add(score);
        }
        List<BigDecimal> weights = points.stream().map(RubricPointView::weight).toList();
        return new ParseRubricResponse(points, weights, List.of());
    }

    @Transactional
    public UpdatePointsResponse savePoints(String rubricId, List<RubricPointRequest> requests) {
        Rubric rubric = find(rubricId);
        requireDraft(rubric);
        BigDecimal requestedTotal = requests.stream().map(RubricPointRequest::weight).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (requestedTotal.compareTo(BigDecimal.valueOf(100)) != 0) {
            throw BusinessException.badRequest("权重合计必须为100%，当前为 " + requestedTotal);
        }
        BigDecimal available = availableScore(rubric);
        BigDecimal pointTotal = requests.stream().map(RubricPointRequest::maxScore).reduce(BigDecimal.ZERO, BigDecimal::add);
        requirePointTotal(available, pointTotal);
        java.util.Set<String> suppliedIds = new java.util.HashSet<>();
        java.util.Set<String> ownedIds = pointRepository.findByRubricIdOrderByPointCode(rubricId).stream()
                .map(RubricPoint::getPointId).collect(java.util.stream.Collectors.toSet());
        for (RubricPointRequest request : requests) {
            if (request.maxScore().signum() < 0 || request.maxScore().compareTo(available) > 0) {
                throw BusinessException.badRequest("要点分值必须在0到可分配分之间");
            }
            if (!"LENGTH_COVERAGE".equals(request.pointType()) && request.keywords().isEmpty()) {
                throw BusinessException.badRequest("每个关键词要点至少需要一个关键词");
            }
            if (request.pointId() != null && !request.pointId().isBlank()
                    && (!suppliedIds.add(request.pointId()) || !ownedIds.contains(request.pointId()))) {
                throw BusinessException.badRequest("pointId 必须唯一且属于当前规则");
            }
        }
        ownedIds.forEach(variantRepository::deleteByPointId);
        pointRepository.deleteByRubricId(rubricId);
        pointRepository.flush();
        List<RubricPointView> views = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        int index = 1;
        for (RubricPointRequest request : requests) {
            RubricPoint point = new RubricPoint();
            point.setPointId(request.pointId() == null || request.pointId().isBlank()
                    ? id("P") : request.pointId());
            point.setRubricId(rubricId);
            point.setPointCode("P" + index++);
            point.setPointName(request.pointName());
            point.setPointType(request.pointType());
            point.setWeight(request.weight());
            point.setMaxScore(request.maxScore());
            point.setKeywords(String.join("|", request.keywords()));
            pointRepository.save(point);
            List<String> variants = new ArrayList<>();
            for (String text : request.variants()) {
                if (text == null || text.isBlank()) {
                    continue;
                }
                PointVariant variant = new PointVariant();
                variant.setVariantId(id("V"));
                variant.setPointId(point.getPointId());
                variant.setVariantText(text);
                variant.setMatchType("SYNONYM");
                variant.setConfidence(BigDecimal.valueOf(0.95));
                variant.setEnabled(true);
                variantRepository.save(variant);
                variants.add(text);
            }
            total = total.add(request.weight());
            views.add(view(point, variants));
        }
        rubric.setStatus("DRAFT");
        rubricRepository.save(rubric);
        return new UpdatePointsResponse(rubricId, views, total);
    }

    @Transactional
    public CompileRubricResponse compile(String rubricId, BigDecimal expectedWeightTotal) {
        // Do not load the entity before the batch lock: another compiler may freeze it while we wait.
        String batchId = rubricRepository.findBatchId(rubricId)
                .orElseThrow(() -> BusinessException.notFound("评分规则不存在"));
        List<Rubric> batchRules = rubricRepository.lockBatch(batchId);
        Rubric rubric = batchRules.stream().filter(item -> rubricId.equals(item.getRubricId())).findFirst()
                .orElseThrow(() -> BusinessException.notFound("评分规则不存在"));
        requireDraft(rubric);
        List<RubricPoint> points = pointRepository.findByRubricIdOrderByPointCode(rubricId);
        if (points.isEmpty()) {
            throw BusinessException.badRequest("评分规则至少需要一个要点");
        }
        requirePointTotal(availableScore(rubric), points.stream().map(RubricPoint::getMaxScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        BigDecimal total = points.stream().map(RubricPoint::getWeight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal expected = BigDecimal.valueOf(100);
        if (expectedWeightTotal == null || expectedWeightTotal.compareTo(expected) != 0) {
            throw BusinessException.badRequest("expectedWeightTotal 必须等于100");
        }
        if (total.compareTo(expected) != 0) {
            throw BusinessException.badRequest("权重合计必须等于 " + expected + "，当前为 " + total);
        }
        int nextVersion = batchRules.stream().filter(item -> "FROZEN".equals(item.getStatus()))
                .mapToInt(Rubric::getVersion).max().orElse(0) + 1;
        rubric.setVersion(nextVersion);
        rubric.setStatus("FROZEN");
        rubric.setCompiledAt(LocalDateTime.now());
        String payload = payload(rubric, points);
        rubric.setChecksum(sha256(payload));
        rubricRepository.save(rubric);

        RubricVersionSnapshot snapshot = new RubricVersionSnapshot();
        snapshot.setSnapshotId(id("RV"));
        snapshot.setRubricId(rubricId);
        snapshot.setVersion(rubric.getVersion());
        snapshot.setPayload(payload);
        snapshot.setChecksum(rubric.getChecksum());
        snapshot.setCompiledAt(rubric.getCompiledAt());
        versionRepository.save(snapshot);
        return new CompileRubricResponse(rubricId, rubric.getVersion(), rubric.getChecksum(),
                rubric.getCompiledAt().atZone(ZoneId.systemDefault()).toOffsetDateTime());
    }

    @Transactional(readOnly = true)
    public RubricVersionView version(String rubricId, int version) {
        RubricVersionSnapshot snapshot = versionRepository.findByRubricIdAndVersion(rubricId, version)
                .orElseThrow(() -> BusinessException.notFound("评分规则版本不存在"));
        try {
            RubricVersionView view = objectMapper.readValue(snapshot.getPayload(), RubricVersionView.class);
            return new RubricVersionView(view.rubricId(), view.batchId(), view.rubricVersion(),
                    snapshot.getChecksum(),
                    snapshot.getCompiledAt().atZone(ZoneId.systemDefault()).toOffsetDateTime(),
                    view.points(), view.baseScore(), view.maxScore());
        } catch (Exception ex) {
            throw new IllegalStateException("评分规则快照损坏", ex);
        }
    }

    @Transactional(readOnly = true)
    public RubricVersionView currentForBatch(String batchId) {
        Rubric rubric = rubricRepository.findFirstByBatchIdAndStatusOrderByVersionDescCompiledAtDescRubricIdDesc(batchId, "FROZEN")
                .orElseThrow(() -> BusinessException.notFound("该批次还没有冻结的评分规则"));
        return version(rubric.getRubricId(), rubric.getVersion());
    }

    private Rubric find(String rubricId) {
        return rubricRepository.findForUpdate(rubricId)
                .orElseThrow(() -> BusinessException.notFound("评分规则不存在"));
    }

    private void requireDraft(Rubric rubric) {
        if (!"DRAFT".equals(rubric.getStatus())) {
            throw BusinessException.badRequest("规则已冻结；请为批次创建新草稿，冻结版本不可修改");
        }
    }

    private String classify(String name) {
        if (name.contains("原理") || name.contains("原因") || name.contains("机制")) return "PRINCIPLE";
        if (name.contains("意义") || name.contains("作用") || name.contains("价值")) return "SIGNIFICANCE";
        if (name.contains("实际") || name.contains("案例") || name.contains("应用") || name.contains("结合")) return "APPLICATION";
        if (name.contains("对策") || name.contains("措施") || name.contains("建议") || name.contains("方法")) return "COUNTERMEASURE";
        if (name.contains("拓展") || name.contains("创新") || name.contains("延伸")) return "EXTENSION";
        return "CONCEPT";
    }

    private String payload(Rubric rubric, List<RubricPoint> points) {
        RubricVersionView view = new RubricVersionView(rubric.getRubricId(), rubric.getBatchId(),
                rubric.getVersion(), "", null, points.stream().map(point -> view(point, variants(point.getPointId()))).toList(),
                com.swu.smartgrade.common.util.ScorePolicy.baseScore(rubric.getMaxScore(), rubric.getBaseScore()), rubric.getMaxScore());
        try {
            return objectMapper.writeValueAsString(view);
        } catch (Exception ex) {
            throw new IllegalStateException("评分规则无法生成快照", ex);
        }
    }

    private RubricPointView view(RubricPoint point, List<String> variants) {
        return new RubricPointView(point.getPointId(), point.getPointCode(), point.getPointName(),
                point.getPointType(), split(point.getKeywords()), variants, point.getWeight(), point.getMaxScore());
    }

    private BigDecimal availableScore(Rubric rubric) {
        return rubric.getMaxScore().subtract(com.swu.smartgrade.common.util.ScorePolicy.baseScore(
                rubric.getMaxScore(), rubric.getBaseScore()));
    }

    private void requirePointTotal(BigDecimal available, BigDecimal actual) {
        if (actual.compareTo(available) != 0) {
            throw BusinessException.badRequest("要点分值合计必须等于满分减基础分（" + available.toPlainString()
                    + "），当前为 " + actual.toPlainString());
        }
    }

    private List<String> variants(String pointId) {
        return variantRepository.findByPointId(pointId).stream()
                .filter(item -> Boolean.TRUE.equals(item.getEnabled()))
                .map(PointVariant::getVariantText)
                .toList();
    }

    private List<String> split(String value) {
        return value == null || value.isBlank() ? List.of() : List.of(value.split("\\|"));
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }

    private String id(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }
}
