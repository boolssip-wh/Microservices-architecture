package com.swu.smartgrade.grading;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.swu.smartgrade")
public class GradingServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(GradingServiceApplication.class, args);
    }
}
