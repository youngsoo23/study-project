package com.young.studyproject.common.config;

import com.young.studyproject.outbox.domain.PaymentEventTopics;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Kafka Listener 예외 처리 공통 설정.
 *
 * <p>Spring Boot는 컨텍스트에 있는 단일 CommonErrorHandler 빈을 자동 구성된
 * ConcurrentKafkaListenerContainerFactory에 연결해준다. 그래서 모든 @KafkaListener가
 * 별도 설정 없이 이 재시도/DLT 정책을 공유한다.
 *
 * <p>DeadLetterPublishingRecoverer 기본 목적지 계산 규칙은 "{원본 토픽}-dlt"라서,
 * 우리가 원하는 "payment-completed.DLT"(PaymentEventTopics 상수, DocumentDltConsumer가 구독하는 토픽)와
 * 다르다. 목적지를 직접 상수로 고정해 두 값을 일치시킨다.
 */
@Configuration
public class KafkaConfig {

    @Bean
    public CommonErrorHandler kafkaErrorHandler(KafkaOperations<Object, Object> kafkaOperations) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaOperations,
                (record, exception) -> new TopicPartition(PaymentEventTopics.PAYMENT_COMPLETED_DLT, -1));

        // 1초 간격으로 2번 더 재시도(최초 시도 포함 총 3회) 후에도 실패하면 DLT로 보낸다.
        FixedBackOff backOff = new FixedBackOff(1000L, 2L);

        return new DefaultErrorHandler(recoverer, backOff);
    }
}
