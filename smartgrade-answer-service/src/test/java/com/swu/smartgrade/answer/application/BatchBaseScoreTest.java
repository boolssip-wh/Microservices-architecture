package com.swu.smartgrade.answer.application;

import com.swu.smartgrade.answer.api.dto.CreateBatchRequest;
import com.swu.smartgrade.answer.domain.*;
import com.swu.smartgrade.answer.infrastructure.client.*;
import com.swu.smartgrade.common.api.BusinessException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.math.BigDecimal;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class BatchBaseScoreTest {
    private final BatchRepository batches = mock(BatchRepository.class);
    private final BatchApplicationService service = new BatchApplicationService(batches,
            mock(AnswerRepository.class), mock(RubricClient.class), mock(ReviewClient.class));

    @Test
    void savesCustomBaseAndReturnsItToTheClient() {
        var response = service.create(1L, new CreateBatchRequest("考试名称", "Q1", "题目标题", "题干",
                new BigDecimal("20"), new BigDecimal("8")));
        ArgumentCaptor<Batch> saved = ArgumentCaptor.forClass(Batch.class);
        verify(batches).save(saved.capture());
        assertThat(saved.getValue().getBaseScore()).isEqualByComparingTo("8");
        assertThat(response.baseScore()).isEqualByComparingTo("8");
    }

    @Test
    void legacyRequestDefaultsToMaximumWhenMaximumIsBelowTen() {
        var response = service.create(1L, new CreateBatchRequest("考试名称", "Q1", "题目标题", "题干",
                new BigDecimal("5")));
        assertThat(response.baseScore()).isEqualByComparingTo("5");
    }

    @Test
    void rejectsOutOfRangeBaseBeforeSavingAnything() {
        for (String base : new String[]{"-1", "21", "1.001"}) {
            assertThatThrownBy(() -> service.create(1L, new CreateBatchRequest("考试名称", "Q1", "题目标题", "题干",
                    new BigDecimal("20"), new BigDecimal(base)))).isInstanceOf(BusinessException.class);
        }
        verifyNoInteractions(batches);
    }
}
