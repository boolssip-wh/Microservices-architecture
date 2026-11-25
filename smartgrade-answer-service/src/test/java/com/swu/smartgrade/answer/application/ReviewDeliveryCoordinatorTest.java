package com.swu.smartgrade.answer.application;

import com.swu.smartgrade.answer.domain.Answer;
import com.swu.smartgrade.answer.domain.AnswerRepository;
import com.swu.smartgrade.answer.infrastructure.client.ReviewClient;
import com.swu.smartgrade.common.api.ApiResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReviewDeliveryCoordinatorTest {
    private final ReviewClient reviews = mock(ReviewClient.class);
    private final AnswerRepository answers = mock(AnswerRepository.class);
    private final ReviewDeliveryCoordinator coordinator = new ReviewDeliveryCoordinator(reviews, answers, false);

    @Test
    void successCommitsReviewAndAnswerStatusTogether() {
        Answer answer = answer("A-SEATA-1");
        when(reviews.create(any())).thenReturn(
                ApiResponse.ok(new ReviewClient.CreatedResult("R-1", "WAITING_REVIEW"), "trace-1"));

        coordinator.deliver(request("A-SEATA-1"), answer, "WAITING_REVIEW");

        assertThat(answer.getStatus()).isEqualTo("WAITING_REVIEW");
        verify(answers).save(answer);
    }

    @Test
    void secondSuccessIsIdempotentAtRemoteBoundary() {
        Answer answer = answer("A-SEATA-2");
        when(reviews.create(any())).thenReturn(
                ApiResponse.ok(new ReviewClient.CreatedResult("R-2", "WAITING_REVIEW"), "trace-2"));

        coordinator.deliver(request("A-SEATA-2"), answer, "WAITING_REVIEW");
        coordinator.deliver(request("A-SEATA-2"), answer, "WAITING_REVIEW");

        assertThat(answer.getStatus()).isEqualTo("WAITING_REVIEW");
        verify(answers, org.mockito.Mockito.times(2)).save(answer);
    }

    @Test
    void remoteReviewFailureDoesNotChangeAnswerStatus() {
        Answer answer = answer("A-SEATA-3");
        when(reviews.create(any())).thenThrow(new IllegalStateException("review unavailable"));

        assertThatThrownBy(() -> coordinator.deliver(request("A-SEATA-3"), answer, "WAITING_REVIEW"))
                .isInstanceOf(IllegalStateException.class);

        assertThat(answer.getStatus()).isEqualTo("RUNNING");
        verify(answers, never()).save(answer);
    }

    @Test
    void localAnswerWriteFailureEscapesAsGlobalRollbackTrigger() {
        Answer answer = answer("A-SEATA-4");
        when(reviews.create(any())).thenReturn(
                ApiResponse.ok(new ReviewClient.CreatedResult("R-4", "WAITING_REVIEW"), "trace-4"));
        when(answers.save(answer)).thenThrow(new IllegalStateException("answer write failed"));

        assertThatThrownBy(() -> coordinator.deliver(request("A-SEATA-4"), answer, "WAITING_REVIEW"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("answer write failed");

        verify(reviews).create(any());
        verify(answers).save(answer);
    }

    private Answer answer(String answerId) {
        Answer answer = new Answer();
        answer.setAnswerId(answerId);
        answer.setBatchId("B-SEATA");
        answer.setStudentNo("2026999");
        answer.setStatus("RUNNING");
        return answer;
    }

    private ReviewClient.CreateResultRequest request(String answerId) {
        return new ReviewClient.CreateResultRequest(
                answerId, "B-SEATA", "2026999", "有理想、敢担当、能吃苦、肯奋斗",
                1L, 1, new BigDecimal("20"), new BigDecimal("20"),
                BigDecimal.ONE, BigDecimal.ONE, List.of(), "WAITING_REVIEW",
                List.of(), new BigDecimal("10"));
    }
}
