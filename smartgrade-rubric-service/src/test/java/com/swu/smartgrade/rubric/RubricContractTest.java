package com.swu.smartgrade.rubric;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swu.smartgrade.common.api.BusinessException;
import com.swu.smartgrade.rubric.api.dto.RubricPointRequest;
import com.swu.smartgrade.rubric.application.RubricApplicationService;
import com.swu.smartgrade.rubric.domain.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class RubricContractTest {
    private final RubricRepository rubrics = mock(RubricRepository.class);
    private final RubricPointRepository points = mock(RubricPointRepository.class);
    private final RubricVersionRepository versions = mock(RubricVersionRepository.class);
    private final RubricApplicationService service = new RubricApplicationService(rubrics, points,
            mock(PointVariantRepository.class), versions, new ObjectMapper().findAndRegisterModules());

    private Rubric rubric(String status) {
        Rubric rubric = new Rubric();
        rubric.setRubricId("R1"); rubric.setBatchId("B1"); rubric.setVersion(0);
        rubric.setStatus(status); rubric.setMaxScore(new BigDecimal("20"));
        when(rubrics.findForUpdate("R1")).thenReturn(Optional.of(rubric));
        when(rubrics.findBatchId("R1")).thenReturn(Optional.of("B1"));
        when(rubrics.lockBatch("B1")).thenReturn(List.of(rubric));
        return rubric;
    }

    @Test
    void rejectsMutationOfFrozenRules() {
        rubric("FROZEN");
        assertThatThrownBy(() -> service.parse("R1", "概念：测试关键词"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("冻结");
        assertThatThrownBy(() -> service.compile("R1", new BigDecimal("100")))
                .isInstanceOf(BusinessException.class);
        verifyNoInteractions(points);
    }

    @Test
    void refusesIncompleteWeightsBeforeDeletingPoints() {
        rubric("DRAFT");
        var point = new RubricPointRequest(null, "测试要点", "CONCEPT", List.of("概念"), List.of(),
                new BigDecimal("40"), new BigDecimal("5"));
        assertThatThrownBy(() -> service.savePoints("R1", List.of(point)))
                .isInstanceOf(BusinessException.class).hasMessageContaining("100%");
        verifyNoInteractions(points);
    }

    @Test
    void classifiesAllSixDocumentedContentCategories() {
        rubric("DRAFT");
        var result = service.parse("R1", "概念：定义\n原理：规律\n意义：价值\n联系实际：案例\n对策：措施\n拓展：创新");
        assertThat(result.points()).extracting(p -> p.pointType()).containsExactly(
                "CONCEPT", "PRINCIPLE", "SIGNIFICANCE", "APPLICATION", "COUNTERMEASURE", "EXTENSION");
        assertThat(result.points().stream().map(p -> p.weight()).reduce(BigDecimal.ZERO, BigDecimal::add))
                .isEqualByComparingTo("100");
    }

    @Test
    void newDraftUsesNextBatchVersionAndPreservesThePreviousSnapshot() {
        Rubric draft = rubric("DRAFT");
        Rubric frozen = new Rubric();
        frozen.setRubricId("R-OLD"); frozen.setBatchId("B1"); frozen.setStatus("FROZEN"); frozen.setVersion(7);
        Rubric otherDraft = new Rubric();
        otherDraft.setRubricId("R-DRAFT"); otherDraft.setBatchId("B1"); otherDraft.setStatus("DRAFT"); otherDraft.setVersion(99);
        when(rubrics.lockBatch("B1")).thenReturn(List.of(frozen, draft, otherDraft));
        RubricPoint point = new RubricPoint();
        point.setPointId("P1"); point.setPointCode("P01"); point.setPointName("概念");
        point.setPointType("CONCEPT"); point.setWeight(new BigDecimal("100"));
        point.setMaxScore(new BigDecimal("10")); point.setKeywords("概念");
        when(points.findByRubricIdOrderByPointCode("R1")).thenReturn(List.of(point));

        var result = service.compile("R1", new BigDecimal("100"));

        assertThat(result.rubricVersion()).isEqualTo(8);
        assertThat(frozen.getVersion()).isEqualTo(7);
        assertThat(otherDraft.getStatus()).isEqualTo("DRAFT");
        ArgumentCaptor<RubricVersionSnapshot> snapshot = ArgumentCaptor.forClass(RubricVersionSnapshot.class);
        verify(versions).save(snapshot.capture());
        assertThat(snapshot.getValue().getVersion()).isEqualTo(8);
        assertThat(snapshot.getValue().getPayload()).contains("\"rubricVersion\":8");
    }

    @Test
    void compilerRechecksStatusAfterTakingBatchLock() {
        Rubric frozenByAnotherRequest = rubric("FROZEN");
        frozenByAnotherRequest.setVersion(1);
        assertThatThrownBy(() -> service.compile("R1", new BigDecimal("100")))
                .isInstanceOf(BusinessException.class).hasMessageContaining("冻结");
        verifyNoInteractions(points, versions);
    }

    @Test
    void customBaseLeavesOnlyTheRemainingScoreForPoints() {
        Rubric rubric = rubric("DRAFT");
        rubric.setBaseScore(new BigDecimal("8"));
        var parsed = service.parse("R1", "概念：关键词甲\n原理：关键词乙");
        assertThat(parsed.points()).extracting(p -> p.maxScore().toPlainString()).containsExactly("6.00", "6.00");
        var oversized = new RubricPointRequest(null, "概念要点", "CONCEPT", List.of("概念"), List.of(),
                new BigDecimal("100"), new BigDecimal("20"));
        assertThatThrownBy(() -> service.savePoints("R1", List.of(oversized)))
                .isInstanceOf(BusinessException.class).hasMessageContaining("满分减基础分");
        verify(points, never()).deleteByRubricId(anyString());
    }
}
