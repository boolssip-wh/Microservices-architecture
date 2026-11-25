package com.swu.smartgrade.rubric;

import com.swu.smartgrade.common.api.ApiResponse;
import com.swu.smartgrade.common.api.BusinessException;
import com.swu.smartgrade.common.security.AuthPrincipal;
import com.swu.smartgrade.rubric.api.dto.CreateRubricRequest;
import com.swu.smartgrade.rubric.application.RubricAccessService;
import com.swu.smartgrade.rubric.domain.RubricRepository;
import com.swu.smartgrade.rubric.infrastructure.BatchAccessClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RubricBaseScoreAccessTest {
    private final BatchAccessClient batches = mock(BatchAccessClient.class);
    private final RubricAccessService service = new RubricAccessService(mock(RubricRepository.class),
            batches, "local-service-token");

    @BeforeEach
    void bindTeacher() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(AuthPrincipal.ATTR, new AuthPrincipal(1L, "teacher", "教师", "TEACHER"));
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        when(batches.detail(eq("B1"), eq("1"), eq("TEACHER"), any(), eq("local-service-token")))
                .thenReturn(ApiResponse.ok(Map.<String, Object>of("maxScore", 20, "baseScore", 8), "trace-test"));
    }

    @AfterEach
    void clearTeacher() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void legacyRuleRequestInheritsTheBatchCustomBase() {
        var request = service.requireCreate(new CreateRubricRequest("B1", "概念：关键词", new BigDecimal("20")));
        assertThat(request.baseScore()).isEqualByComparingTo("8");
    }

    @Test
    void rejectsRuleBaseThatDiffersFromItsBatch() {
        assertThatThrownBy(() -> service.requireCreate(new CreateRubricRequest("B1", "概念：关键词",
                new BigDecimal("20"), BigDecimal.TEN)))
                .isInstanceOf(BusinessException.class).hasMessageContaining("基础分必须与批次基础分一致");
    }
}
