package com.swu.smartgrade.report.config;

import com.swu.smartgrade.common.api.TraceId;
import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class FeignTraceConfiguration {
    @Bean
    RequestInterceptor feignTraceInterceptor(@Value("${SERVICE_TOKEN:local-service-token}") String serviceToken) {
        return template -> {
            template.header(TraceId.HEADER, TraceId.current());
            template.header("X-Service-Token", serviceToken);
            if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
                for (String header : java.util.List.of("X-User-Id", "X-Username", "X-Real-Name", "X-Role")) {
                    String value = attributes.getRequest().getHeader(header);
                    if (value != null) template.header(header, value);
                }
            }
        };
    }
}
