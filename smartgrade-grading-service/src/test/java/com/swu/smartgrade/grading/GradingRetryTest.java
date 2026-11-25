package com.swu.smartgrade.grading;

import com.swu.smartgrade.common.storage.ObjectStorage;
import com.swu.smartgrade.grading.application.GradingApplicationService;
import com.swu.smartgrade.grading.application.ExplainableMatcher;
import com.swu.smartgrade.grading.config.ScoringProperties;
import com.swu.smartgrade.grading.domain.*;
import com.swu.smartgrade.grading.api.dto.RetryRequest;
import com.swu.smartgrade.grading.infrastructure.ocr.VisionLlmOcrClient;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class GradingRetryTest {
    @Test
    void retryKeyIsIdempotentAndFourthRetryBecomesManual() {
        GradingTaskRepository repository = mock(GradingTaskRepository.class);
        var props = new ScoringProperties();
        var service = new GradingApplicationService(repository, new ExplainableMatcher(props), props,
                mock(VisionLlmOcrClient.class), mock(ObjectStorage.class),
                mock(GradingResultRepository.class), mock(HitEvidenceRepository.class));
        var task = new GradingTask();
        task.setTaskId("T1");
        task.setStatus("FAILED");
        task.setRetryCount(0);
        when(repository.findForUpdate("T1")).thenReturn(Optional.of(task));
        var first = new RetryRequest("T1", "retry", "k1");
        assertThat(service.retry("T1", first).retryCount()).isEqualTo(1);
        assertThat(service.retry("T1", first).retryCount()).isEqualTo(1);
        for (int i = 2; i <= 3; i++) {
            task.setStatus("FAILED");
            assertThat(service.retry("T1", new RetryRequest("T1", "retry", "k" + i)).retryCount()).isEqualTo(i);
        }
        task.setStatus("FAILED");
        assertThat(service.retry("T1", new RetryRequest("T1", "retry", "k4")).status()).isEqualTo("MANUAL_REVIEW");
        assertThat(task.getRetryCount()).isEqualTo(3);
        when(repository.findById("missing")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get("missing"))
                .isInstanceOf(com.swu.smartgrade.common.api.BusinessException.class);
    }
}
