package com.swu.smartgrade.grading.infrastructure.events;

import com.swu.smartgrade.common.api.ApiResponse;
import com.swu.smartgrade.common.api.TraceId;
import feign.Request;
import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import java.util.concurrent.TimeUnit;

@FeignClient(name = "smartgrade-answer-service", url = "${ANSWER_SERVICE_URL:}",
        contextId = "gradingImportJobs", configuration = ImportJobClient.Configuration.class)
public interface ImportJobClient {
    @PostMapping("/internal/import-jobs/{jobId}/process")
    ApiResponse<String> process(@PathVariable("jobId") String jobId);

    class Configuration {
        @Bean
        public RequestInterceptor serviceCredentials(@Value("${SERVICE_TOKEN:local-service-token}") String token) {
            return template -> {
                template.header("X-Service-Token", token);
                template.header(TraceId.HEADER, TraceId.current());
            };
        }
        @Bean
        public Request.Options importJobTimeouts() {
            return new Request.Options(5, TimeUnit.SECONDS, 120, TimeUnit.SECONDS, true);
        }
    }
}
