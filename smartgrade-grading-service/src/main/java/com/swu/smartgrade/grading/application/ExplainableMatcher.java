package com.swu.smartgrade.grading.application;

import com.swu.smartgrade.common.util.AnswerNormalizer;
import com.swu.smartgrade.grading.api.dto.HitView;
import com.swu.smartgrade.grading.api.dto.MatchResult;
import com.swu.smartgrade.grading.api.dto.RubricPoint;
import com.swu.smartgrade.grading.config.ScoringProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Component
public class ExplainableMatcher {
    private final ScoringProperties properties;

    public ExplainableMatcher(ScoringProperties properties) {
        this.properties = properties;
    }

    public MatchResult match(String text, List<RubricPoint> points,
                             BigDecimal answerCoverageRatio, BigDecimal maxScore) {
        BigDecimal base = maxScore == null ? properties.getBaseScore() : properties.getBaseScore().min(maxScore);
        return match(text, points, answerCoverageRatio, maxScore, base);
    }

    public MatchResult match(String text, List<RubricPoint> points,
                             BigDecimal answerCoverageRatio, BigDecimal maxScore, BigDecimal baseScore) {
        BigDecimal initialScore = maxScore == null ? (baseScore == null ? properties.getBaseScore() : baseScore)
                : com.swu.smartgrade.common.util.ScorePolicy.baseScore(maxScore, baseScore);
        String safeText = text == null ? "" : text;
        if (safeText.isBlank()) {
            return new MatchResult(List.of(), BigDecimal.ZERO, BigDecimal.ZERO);
        }
        List<HitView> hits = new ArrayList<>();
        BigDecimal score = initialScore;
        BigDecimal confidenceTotal = BigDecimal.ZERO;
        for (RubricPoint point : points) {
            HitView hit = "LENGTH_COVERAGE".equalsIgnoreCase(point.pointType())
                    ? checkCoverage(safeText, point, answerCoverageRatio)
                    : findHit(safeText, point);
            if (hit != null) {
                hits.add(hit);
                score = score.add(point.maxScore());
                confidenceTotal = confidenceTotal.add(hit.confidence());
            }
        }
        BigDecimal ceiling = maxScore == null ? score : maxScore;
        score = score.min(ceiling);
        if (properties.isRoundToInteger()) {
            score = score.setScale(0, RoundingMode.HALF_UP);
        } else {
            score = score.setScale(2, RoundingMode.HALF_UP);
        }
        score = score.min(ceiling);
        BigDecimal confidence = points.isEmpty() ? BigDecimal.ZERO
                : confidenceTotal.divide(BigDecimal.valueOf(points.size()), 2, RoundingMode.HALF_UP);
        return new MatchResult(hits, score, confidence);
    }

    private HitView findHit(String text, RubricPoint point) {
        List<SearchTerm> terms = new ArrayList<>();
        if (point.keywords() != null) {
            point.keywords().stream().filter(this::present)
                    .forEach(item -> terms.add(new SearchTerm(item, "EXACT", new BigDecimal("0.99"))));
        }
        if (point.variants() != null) {
            point.variants().stream().filter(this::present)
                    .forEach(item -> terms.add(new SearchTerm(item, "VARIANT", new BigDecimal("0.92"))));
        }
        for (SearchTerm term : terms) {
            String normalizedTerm = AnswerNormalizer.normalize(term.text());
            int index = AnswerNormalizer.normalize(text).indexOf(normalizedTerm);
            if (index >= 0) {
                // Matching strips whitespace and folds full-width characters; map back to the original.
                int originalIndex = originalOffset(text, index);
                String sentence = sentenceAt(text, originalIndex);
                int sentenceStart = text.lastIndexOf(sentence, originalIndex);
                if (sentenceStart < 0) sentenceStart = text.indexOf(sentence);
                return new HitView(point.pointId(), point.pointName(), term.matchType(), sentence,
                        Math.max(0, sentenceStart), Math.max(0, sentenceStart) + sentence.length(),
                        term.confidence());
            }
        }
        return null;
    }

    private int originalOffset(String text, int normalizedOffset) {
        int normalizedLength = 0;
        for (int offset = 0; offset < text.length();) {
            int count = Character.charCount(text.codePointAt(offset));
            String piece = AnswerNormalizer.normalize(text.substring(offset, offset + count));
            if (normalizedLength + piece.length() > normalizedOffset) return offset;
            normalizedLength += piece.length();
            offset += count;
        }
        return Math.max(0, text.length() - 1);
    }

    private HitView checkCoverage(String text, RubricPoint point, BigDecimal answerCoverageRatio) {
        BigDecimal ratio = answerCoverageRatio == null ? BigDecimal.ZERO : answerCoverageRatio;
        if (ratio.compareTo(properties.getMinAnswerAreaRatio()) < 0) {
            return null;
        }
        String evidence = "卷面作答覆盖率 " + ratio.multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP) + "%，达到至少 "
                + properties.getMinAnswerAreaRatio().multiply(BigDecimal.valueOf(100))
                .stripTrailingZeros().toPlainString() + "% 的要求。";
        return new HitView(point.pointId(), point.pointName(), "LENGTH",
                evidence, 0, text.length(), properties.getMinAnswerAreaRatio());
    }

    private String sentenceAt(String text, int index) {
        int start = index;
        while (start > 0 && "。！？!?；;\n".indexOf(text.charAt(start - 1)) < 0) {
            start--;
        }
        int end = index;
        while (end < text.length() && "。！？!?；;\n".indexOf(text.charAt(end)) < 0) {
            end++;
        }
        if (end < text.length()) {
            end++;
        }
        return text.substring(Math.min(start, text.length()), Math.min(end, text.length())).trim();
    }

    private boolean present(String value) {
        return value != null && !value.isBlank();
    }

    private record SearchTerm(String text, String matchType, BigDecimal confidence) {
    }
}
