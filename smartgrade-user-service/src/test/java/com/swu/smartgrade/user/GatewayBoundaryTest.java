package com.swu.smartgrade.user;

import com.swu.smartgrade.common.api.BusinessException;
import com.swu.smartgrade.common.security.AuthHeaderInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GatewayBoundaryTest {
    private final AuthHeaderInterceptor interceptor =
            new AuthHeaderInterceptor("service-test-token", "gateway-test-token");

    @Test
    void directApiCallWithoutGatewayCredentialIsRejected() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/auth/me");
        request.addHeader("X-User-Id", "1");

        assertThatThrownBy(() -> interceptor.preHandle(
                request, new MockHttpServletResponse(), new Object()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("统一网关");
    }

    @Test
    void gatewayCredentialAllowsIdentityForwarding() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/auth/me");
        request.addHeader("X-Gateway-Token", "gateway-test-token");
        request.addHeader("X-User-Id", "1");
        request.addHeader("X-Role", "TEACHER");

        assertThat(interceptor.preHandle(
                request, new MockHttpServletResponse(), new Object())).isTrue();
        assertThat(request.getAttribute("smartgrade.principal")).isNotNull();
    }

    @Test
    void internalCallWithoutServiceCredentialIsRejected() {
        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/internal/teachers/1/comment-profile");

        assertThatThrownBy(() -> interceptor.preHandle(
                request, new MockHttpServletResponse(), new Object()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("服务凭据");
    }
}
