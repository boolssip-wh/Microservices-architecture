package com.swu.smartgrade.grading.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Getter
@Setter
/**
 * 从 Nacos 的 smartgrade-common.yaml 绑定评分阈值；@RefreshScope 使配置修改后无需重启。
 */
@Component
@RefreshScope
@ConfigurationProperties(prefix = "smartgrade.grading")
public class ScoringProperties {
    private BigDecimal baseScore = new BigDecimal("10");
    private BigDecimal minAnswerAreaRatio = new BigDecimal("0.50");
    private int minimumTextCharacters = 240;
    private boolean roundToInteger = true;
    private Ocr ocr = new Ocr();

    @Getter
    @Setter
    public static class Ocr {
        private String provider = "vision-llm";
        private String baseUrl = "";
        private String model = "";
        private String apiKey = "";
        private int timeoutSeconds = 90;
        private long maxImageBytes = 15L * 1024 * 1024;

        public boolean isConfigured() {
            return provider != null && !provider.isBlank()
                    && baseUrl != null && !baseUrl.isBlank()
                    && model != null && !model.isBlank()
                    && apiKey != null && !apiKey.isBlank();
        }
    }
}
