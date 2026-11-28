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

/**
 * D1 成功/失败测试证据：独立验证远程复核写入和本地答卷状态更新的协作边界。
 * 运行后 IDEA 的 Tests 标签应显示 5 个方法全部通过。
 */
class ReviewDeliveryCoordinatorTest {
    private final ReviewClient reviews = mock(ReviewClient.class);
    private final AnswerRepository answers = mock(AnswerRepository.class);
    private final ReviewDeliveryCoordinator coordinator = new ReviewDeliveryCoordinator(reviews, answers, false, 0);

    @Test
    void successCommitsReviewAndAnswerStatusTogether() {
        // 成功场景：review 远程写入成功后，answer 本地状态同步变为 WAITING_REVIEW。
        Answer answer = answer("A-SEATA-1");
        when(reviews.create(any())).thenReturn(
                ApiResponse.ok(new ReviewClient.CreatedResult("R-1", "WAITING_REVIEW"), "trace-1"));

        coordinator.deliver(request("A-SEATA-1"), answer, "WAITING_REVIEW");

        assertThat(answer.getStatus()).isEqualTo("WAITING_REVIEW");
        verify(answers).save(answer);
    }

    @Test
    void secondSuccessIsIdempotentAtRemoteBoundary() {
        // 重复投递场景：验证协调器可重复接收同一业务请求，远程边界需保持幂等。
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
        // 远程失败场景：review 写入抛错时，answer 不应保存为待复核状态。
        Answer answer = answer("A-SEATA-3");
        when(reviews.create(any())).thenThrow(new IllegalStateException("review unavailable"));

        assertThatThrownBy(() -> coordinator.deliver(request("A-SEATA-3"), answer, "WAITING_REVIEW"))
                .isInstanceOf(IllegalStateException.class);

        assertThat(answer.getStatus()).isEqualTo("RUNNING");
        verify(answers, never()).save(answer);
    }

    @Test
    void localAnswerWriteFailureEscapesAsGlobalRollbackTrigger() {
        // 本地失败场景：远程写成功后本地保存失败，异常必须向外抛出以触发 Seata 全局回滚。
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

    @Test
    void demoFaultSwitchCanBeEnabledAndDisabledWithoutRestart() {
        // 现场故障开关：不用重启服务即可切换失败注入，便于演示 Seata 活跃事务和回滚。
        Answer answer = answer("A-SEATA-5");
        when(reviews.create(any())).thenReturn(
                ApiResponse.ok(new ReviewClient.CreatedResult("R-5", "WAITING_REVIEW"), "trace-5"));

        coordinator.setFailAfterRemote(true);
        assertThatThrownBy(() -> coordinator.deliver(request("A-SEATA-5"), answer, "WAITING_REVIEW"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SEATA_DEMO_FAIL_AFTER_REMOTE");
        coordinator.setFailAfterRemote(false);
        coordinator.deliver(request("A-SEATA-5"), answer, "WAITING_REVIEW");

        assertThat(answer.getStatus()).isEqualTo("WAITING_REVIEW");
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
