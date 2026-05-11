package com.finlearn.quizservice.quizsession.infrastructure.outbox;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Outbox Relay.
 * PENDING 상태의 Outbox 이벤트를 주기적으로 읽어 Kafka로 발행하고 PUBLISHED로 마킹한다.
 * Kafka 전송 실패 시 PENDING 상태 유지 → 다음 사이클에 재시도 (at-least-once 보장).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventRelay {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void relay() {
        List<OutboxEvent> pendingEvents = outboxEventRepository
                .findTop100ByStatusOrderByCreatedAtAsc(OutboxEvent.OutboxStatus.PENDING);

        if (pendingEvents.isEmpty()) {
            return;
        }

        for (OutboxEvent event : pendingEvents) {
            try {
                // JSON 문자열을 Map으로 역직렬화하여 Kafka로 전송
                Map<String, Object> payload = objectMapper.readValue(
                        event.getPayload(), new TypeReference<>() {});

                // 동기 전송으로 성공 여부 확인 후 마킹
                kafkaTemplate.send(event.getTopic(), event.getAggregateId(), payload).get();
                event.markPublished();

                log.info("[Outbox] 발행 완료 - topic: {}, id: {}", event.getTopic(), event.getId());

            } catch (Exception e) {
                log.error("[Outbox] 발행 실패 - id: {}, topic: {}", event.getId(), event.getTopic(), e);
                // PENDING 유지 → 다음 사이클에 재시도
            }
        }
    }
}
