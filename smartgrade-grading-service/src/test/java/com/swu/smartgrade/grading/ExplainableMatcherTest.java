package com.swu.smartgrade.grading;

import com.swu.smartgrade.grading.api.dto.RubricPoint;
import com.swu.smartgrade.grading.application.ExplainableMatcher;
import com.swu.smartgrade.grading.config.ScoringProperties;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ExplainableMatcherTest {
    private final ExplainableMatcher matcher = new ExplainableMatcher(new ScoringProperties());

    @Test
    void normalizedKeywordOffsetStillPointsAtOriginalSentence() {
        String text = "前言有很多空格    。\n\n目标 A B C 的具体说明。";
        var result = matcher.match(text, List.of(new RubricPoint("P1", "目标", "CONCEPT",
                List.of("ABC"), List.of(), BigDecimal.valueOf(100), BigDecimal.ONE)),
                BigDecimal.ONE, BigDecimal.TEN);
        var hit = result.hits().get(0);
        assertThat(hit.evidence()).contains("A B C");
        assertThat(text.substring(hit.start(), hit.end())).isEqualTo(hit.evidence());
    }

    @Test
    void returnsEvidenceAndOffsetForKeywordAndVariant() {
        List<RubricPoint> points = List.of(
                new RubricPoint("P1", "解析过程", "KEYWORD",
                        List.of("移项"), List.of("常数项移到右边"), new BigDecimal("50"), new BigDecimal("5")),
                new RubricPoint("P2", "单位", "KEYWORD",
                        List.of("平方米"), List.of(), new BigDecimal("50"), new BigDecimal("5"))
        );
        var result = matcher.match("先把常数项移到右边，然后再计算。答案单位是平方米。",
                points, BigDecimal.ONE, new BigDecimal("20"));

        assertThat(result.hits()).hasSize(2);
        assertThat(result.suggestedScore()).isEqualByComparingTo("20");
        assertThat(result.hits().get(0).evidence()).contains("常数项移到右边");
        assertThat(result.hits().get(0).start()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void explanationCaseStartsAtTenAndAddsKeywordAndCoveragePoints() {
        List<RubricPoint> points = List.of(
                new RubricPoint("P1", "有理想", "KEYWORD",
                        List.of("有理想"), List.of(), new BigDecimal("22.5"), new BigDecimal("2.25")),
                new RubricPoint("P2", "敢担当", "KEYWORD",
                        List.of("敢担当"), List.of(), new BigDecimal("22.5"), new BigDecimal("2.25")),
                new RubricPoint("P5", "作答篇幅", "LENGTH_COVERAGE",
                        List.of(), List.of(), new BigDecimal("10"), BigDecimal.ONE)
        );

        var result = matcher.match("作为入党积极分子，我要做一个有理想、敢担当的青年。",
                points, new BigDecimal("0.62"), new BigDecimal("20"));

        assertThat(result.hits()).extracting(hit -> hit.pointName())
                .containsExactly("有理想", "敢担当", "作答篇幅");
        assertThat(result.suggestedScore()).isEqualByComparingTo("16");
    }

    @Test
    void batchBaseControlsTheRealScoreAndBlankAnswersStillReceiveZero() {
        var points = List.of(new RubricPoint("P1", "目标", "CONCEPT", List.of("命中"),
                List.of(), BigDecimal.valueOf(100), new BigDecimal("12")));
        assertThat(matcher.match("有效但未覆盖关键词", points, BigDecimal.ONE,
                new BigDecimal("20"), new BigDecimal("8")).suggestedScore()).isEqualByComparingTo("8");
        assertThat(matcher.match("命中", points, BigDecimal.ONE,
                new BigDecimal("20"), new BigDecimal("8")).suggestedScore()).isEqualByComparingTo("20");
        assertThat(matcher.match("有效但未覆盖关键词", points, BigDecimal.ONE,
                new BigDecimal("20"), BigDecimal.ZERO).suggestedScore()).isEqualByComparingTo("0");
        assertThat(matcher.match(" ", points, BigDecimal.ONE,
                new BigDecimal("20"), new BigDecimal("8")).suggestedScore()).isEqualByComparingTo("0");
        assertThat(matcher.match("有效作答", List.of(), BigDecimal.ONE,
                new BigDecimal("5"), null).suggestedScore()).isEqualByComparingTo("5");
        assertThat(matcher.match("有效作答", List.of(), BigDecimal.ONE,
                new BigDecimal("5.75"), null).suggestedScore()).isEqualByComparingTo("5.75");
    }
}
