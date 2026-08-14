package com.young.studyproject.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * outbox.application.OutboxPublisher의 @Scheduled가 동작하려면 @EnableScheduling이 있어야 한다.
 * 없으면 @Scheduled는 조용히 무시된다.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
