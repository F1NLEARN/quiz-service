package com.finlearn.quizservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Outbox Relay 등 스케줄러 활성화 설정 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
