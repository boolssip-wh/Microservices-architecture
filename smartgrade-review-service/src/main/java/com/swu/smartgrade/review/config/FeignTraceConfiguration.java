package com.swu.smartgrade.review.config;

import com.swu.smartgrade.common.api.TraceId;
import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignTraceConfiguration {
    @Bean
    RequestInterceptor feignTraceInterceptor(@org.springframework.beans.factory.annotation.Value("${app.security.service-token:${SERVICE_TOKEN:local-service-token}}") String token) {
        return template -> {
            template.header(TraceId.HEADER, TraceId.current());
            template.header("X-Service-Token", token);
            var attrs = org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
            Object value = attrs == null ? null : attrs.getAttribute(
                    com.swu.smartgrade.common.security.AuthPrincipal.ATTR,
                    org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST);
            if (value instanceof com.swu.smartgrade.common.security.AuthPrincipal principal) {
                template.header("X-User-Id", principal.userId().toString());
                template.header("X-Role", principal.role());
            }
        };
    }
}
