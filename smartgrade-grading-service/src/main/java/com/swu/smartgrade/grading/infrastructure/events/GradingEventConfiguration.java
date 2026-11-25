package com.swu.smartgrade.grading.infrastructure.events;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("infra")
@EnableFeignClients(clients = ImportJobClient.class)
public class GradingEventConfiguration {}
