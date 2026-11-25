package com.swu.smartgrade.answer.config;

import com.swu.smartgrade.common.api.TraceId;
import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignTraceConfiguration {
    @Bean
    RequestInterceptor feignTraceInterceptor(@org.springframework.beans.factory.annotation.Value("${app.security.service-token:${SERVICE_TOKEN:local-service-token}}") String serviceToken) {
        return template -> {
            template.header(TraceId.HEADER, TraceId.current());
            template.header("X-Service-Token", serviceToken);
            if (org.springframework.web.context.request.RequestContextHolder.getRequestAttributes()
                    instanceof org.springframework.web.context.request.ServletRequestAttributes attributes) {
                for (String header : java.util.List.of("X-User-Id", "X-Username", "X-Real-Name", "X-Role")) {
                    String value = attributes.getRequest().getHeader(header);
                    if (value != null) template.header(header, value);
                }
            }
        };
    }
}
