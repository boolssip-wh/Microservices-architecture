package com.swu.smartgrade.rubric;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swu.smartgrade.rubric.application.RubricApplicationService;
import com.swu.smartgrade.rubric.domain.PointVariantRepository;
import com.swu.smartgrade.rubric.domain.Rubric;
import com.swu.smartgrade.rubric.domain.RubricPointRepository;
import com.swu.smartgrade.rubric.domain.RubricRepository;
import com.swu.smartgrade.rubric.domain.RubricVersionRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RubricParseDistributionTest {
    @Test
    void distributesWeightAndScoreRemainderToLastPoint() {
        RubricRepository rubricRepository = mock(RubricRepository.class);
        Rubric rubric = new Rubric();
        rubric.setRubricId("R-DEMO");
        rubric.setStatus("DRAFT");
        rubric.setMaxScore(new BigDecimal("20"));
        rubric.setSourceText("demo");
        when(rubricRepository.findForUpdate("R-DEMO")).thenReturn(Optional.of(rubric));
        RubricApplicationService service = new RubricApplicationService(
                rubricRepository,
                mock(RubricPointRepository.class),
                mock(PointVariantRepository.class),
                mock(RubricVersionRepository.class),
                new ObjectMapper()
        );

        var result = service.parse("R-DEMO", """
                人物特征：性格、身份、处境|语言描写|动作描写
                结合原文：引用原文|具体语句|原文细节
                分析作用：表现人物|推动情节|深化主题
                """);

        assertThat(result.points()).hasSize(3);
        assertThat(result.points()).extracting(point -> point.weight().toPlainString())
                .containsExactly("33.33", "33.33", "33.34");
        assertThat(result.points()).extracting(point -> point.maxScore().toPlainString())
                .containsExactly("3.33", "3.33", "3.34");
    }

    @Test
    void recognizesCoverageAsSpecialPointType() {
        RubricRepository rubricRepository = mock(RubricRepository.class);
        Rubric rubric = new Rubric();
        rubric.setRubricId("R-CASE");
        rubric.setStatus("DRAFT");
        rubric.setMaxScore(new BigDecimal("20"));
        when(rubricRepository.findForUpdate("R-CASE")).thenReturn(Optional.of(rubric));
        RubricApplicationService service = new RubricApplicationService(
                rubricRepository,
                mock(RubricPointRepository.class),
                mock(PointVariantRepository.class),
                mock(RubricVersionRepository.class),
                new ObjectMapper()
        );

        var result = service.parse("R-CASE", """
                有理想：有理想|理想信念
                敢担当：敢担当|勇于担当
                能吃苦：能吃苦|艰苦奋斗
                肯奋斗：肯奋斗|不懈奋斗
                作答篇幅：LENGTH_COVERAGE
                """);

        assertThat(result.points()).hasSize(5);
        assertThat(result.points().get(4).pointType()).isEqualTo("LENGTH_COVERAGE");
        assertThat(result.points().get(4).keywords()).isEmpty();
    }
}
