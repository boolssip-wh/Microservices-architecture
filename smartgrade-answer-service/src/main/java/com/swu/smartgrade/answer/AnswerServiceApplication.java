package com.swu.smartgrade.answer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@EnableFeignClients
@SpringBootApplication(scanBasePackages = "com.swu.smartgrade")
public class AnswerServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AnswerServiceApplication.class, args);
    }
}
