package com.swu.smartgrade.review;

import com.swu.smartgrade.common.api.ApiResponse;
import com.swu.smartgrade.review.api.dto.CommentSuggestionRequest;
import com.swu.smartgrade.review.api.dto.HitInput;
import com.swu.smartgrade.review.api.dto.CreateResultRequest;
import com.swu.smartgrade.review.application.ReviewApplicationService;
import com.swu.smartgrade.common.event.DomainEventPublisher;
import com.swu.smartgrade.review.infrastructure.client.UserClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {
        "spring.config.import=",
        "spring.cloud.nacos.config.enabled=false",
        "spring.cloud.nacos.discovery.enabled=false",
        "spring.cloud.nacos.discovery.register-enabled=false",
        "spring.cloud.sentinel.enabled=false"
})
class ReviewOrchestrationIntegrationTest {
    @Autowired
    private ReviewApplicationService service;

    @MockBean
    private UserClient userClient;

    @MockBean
    private DomainEventPublisher eventPublisher;

    @Test
    void commentDraftAndFinalScoreUseTwoRemoteClients() {
        service.create(new CreateResultRequest(
                "A-TEST-001", "B-TEST-001", "2026001", "答案包含移项和检验。",
                1L, 1, new BigDecimal("8"), new BigDecimal("10"), new BigDecimal("0.60"),
                new BigDecimal("0.95"),
                List.of(new HitInput("P1", "移项", "EXACT", "答案包含移项", 4, 6, new BigDecimal("0.99")))
        ));
        when(userClient.profile(1L, "teacher", "教师", "TEACHER")).thenReturn(
                ApiResponse.ok(new UserClient.CommentProfile(
                        10L, 1L, "温和·适中", "温和", "适中", "肯定优先",
                        List.of("继续保持"), List.of("你太差了"), 2), "trace-test")
        );

        var suggestion = service.suggestComment(1L, "A-TEST-001",
                new CommentSuggestionRequest(new BigDecimal("9"), 2, "请突出证据，语气温和"));
        var finalized = service.finalizeScore(1L, "A-TEST-001",
                new com.swu.smartgrade.review.api.dto.FinalScoreRequest(
                        new BigDecimal("9"), "证据充分", "继续保持，注意检验。", 2));

        assertThat(suggestion.suggestedComments()).hasSize(2);
        assertThat(suggestion.evidenceRefs()).isNotEmpty();
        assertThat(suggestion.generationMode()).isEqualTo("TEMPLATE");
        assertThat(finalized.finalScore()).isEqualByComparingTo("9");
        assertThat(service.detail(1L, "A-TEST-001").comment().suggestedText()).isNotBlank();
        assertThat(service.states(1L, "B-TEST-001")).extracting(ReviewApplicationService.ResultState::status)
                .containsExactly("REVIEWED");
        verify(userClient).profile(1L, "teacher", "教师", "TEACHER");
        verify(eventPublisher).publish(any());
    }

    @Test
    void pendingStatusAndDecimalFinalScoreRemainConsistentOnEventReplay() {
        CreateResultRequest request = new CreateResultRequest(
                "A-TEST-002", "B-TEST-002", "2026002", "待人工复核",
                2L, 1, BigDecimal.ZERO, BigDecimal.TEN, BigDecimal.ZERO,
                BigDecimal.ZERO, List.of(), "MANUAL_REVIEW",
                List.of(new CreateResultRequest.ScoringPoint("P-UNHIT", "无人命中的要点")), new BigDecimal("8"));
        service.create(request);
        assertThat(service.detail(2L, "A-TEST-002").baseScore()).isEqualByComparingTo("8");
        var state = service.updateStatus(2L, "A-TEST-002",
                new com.swu.smartgrade.review.api.dto.ReviewStatusRequest("WAITING_REVIEW", "补充证据"));
        assertThat(state.status()).isEqualTo("WAITING_REVIEW");
        service.finalizeScore(2L, "A-TEST-002",
                new com.swu.smartgrade.review.api.dto.FinalScoreRequest(
                        new BigDecimal("8.25"), "人工确认", "需要补充依据。", 0));
        service.create(request);
        assertThat(service.detail(2L, "A-TEST-002").finalScore()).isEqualByComparingTo("8.25");
        assertThat(service.detail(2L, "A-TEST-002").status()).isEqualTo("REVIEWED");
        assertThat(service.detail(2L, "A-TEST-002").baseScore()).isEqualByComparingTo("8");
        assertThat(service.summary("B-TEST-002").pointMissCounts().get(0).missCount()).isEqualTo(1L);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.states(1L, "B-TEST-002"))
                .isInstanceOf(com.swu.smartgrade.common.api.BusinessException.class);
    }
}
