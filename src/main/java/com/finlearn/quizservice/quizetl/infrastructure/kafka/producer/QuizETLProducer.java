package com.finlearn.quizservice.quizetl.infrastructure.kafka.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finlearn.common.exception.InternalServerException;
import com.finlearn.common.security.HeaderUserDetails;
import com.finlearn.quizservice.quizetl.infrastructure.kafka.event.QuizCrawledEvent;
import com.finlearn.quizservice.quizetl.infrastructure.kafka.event.QuizETLTriggeredEvent;
import com.finlearn.quizservice.quizetl.infrastructure.kafka.event.QuizEmbeddedEvent;
import com.finlearn.quizservice.quizsession.infrastructure.outbox.OutboxEvent;
import com.finlearn.quizservice.quizsession.infrastructure.outbox.OutboxEventRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class QuizETLProducer {

    @Value("${kafka.topics.quiz.etl-triggered:finlearn-quiz-etl-triggered}")
    private String topicEtlTriggered;

    @Value("${kafka.topics.quiz.crawled:finlearn-quiz-crawled}")
    private String topicCrawled;

    @Value("${kafka.topics.quiz.embedded:finlearn-quiz-embedded}")
    private String topicEmbedded;

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    // 일관성 유지를 위해 바로 키워드 생성 하지 않고 단순 etl 시작 이벤트를 발행하여 consumer가 처리하도록 함
    public void triggerEtl(Integer count) {
        publishEvent(topicEtlTriggered, "etl-triggered",
                (userId, email, role) -> new QuizETLTriggeredEvent(count, userId, email, role, LocalDateTime.now()));
    }

    public void triggerCrawledResume() {
        publishEvent(topicCrawled, "수동 재개(crawled)",
                (userId, email, role) -> new QuizCrawledEvent(userId, email, role, LocalDateTime.now()));
    }

    public void triggerEmbeddedResume() {
        publishEvent(topicEmbedded, "수동 재개(embedded)",
                (userId, email, role) -> new QuizEmbeddedEvent(userId, email, role, LocalDateTime.now()));
    }

    private void publishEvent(String topic, String logPrefix, EventFactory factory) {
        log.info("{} 이벤트 outbox에 저장 시작", logPrefix);

        UUID userId = null;
        String email = null;
        String role = null;

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof HeaderUserDetails userDetails) {
            userId = userDetails.getUserId();
            email = userDetails.getEmail();
            role = userDetails.getRole();
        }

        Object event = factory.create(userId, email, role);
        String partitionKey = userId != null ? userId.toString() : "SYSTEM";

        try {
            String json = objectMapper.writeValueAsString(event);
            outboxEventRepository.save(OutboxEvent.create(topic, partitionKey, json));
            log.info("{} 이벤트 outbox에 저장 완료", logPrefix);
        } catch (JsonProcessingException e) {
            throw new InternalServerException("이벤트 직렬화 실패: " + e.getMessage());
        }
    }

    @FunctionalInterface
    private interface EventFactory {
        Object create(UUID userId, String email, String role);
    }

}
