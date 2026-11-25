package com.swu.smartgrade.rubric;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = "com.swu.smartgrade")
@EnableFeignClients
public class RubricServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(RubricServiceApplication.class, args);
    }
}
