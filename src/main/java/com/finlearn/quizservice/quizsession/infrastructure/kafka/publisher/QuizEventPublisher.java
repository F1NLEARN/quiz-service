package com.finlearn.quizservice.quizsession.infrastructure.kafka.publisher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finlearn.quizservice.quizsession.domain.event.PointQuizPassed;
import com.finlearn.quizservice.quizsession.domain.event.QuizAnswerSubmitted;
import com.finlearn.quizservice.quizsession.infrastructure.kafka.event.PointQuizPassedEvent;
import com.finlearn.quizservice.quizsession.infrastructure.kafka.event.QuizAnswerSubmittedEvent;
import com.finlearn.quizservice.quizsession.infrastructure.outbox.OutboxEvent;
import com.finlearn.quizservice.quizsession.infrastructure.outbox.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 도메인 이벤트를 수신하여 Outbox 테이블에 저장하는 이벤트 핸들러.
 *
 * @EventListener를 사용하여 도메인 트랜잭션 내에서 동기 실행된다.
 * Outbox 저장이 도메인 변경과 같은 트랜잭션에 묶이므로 메시지 유실이 없다.
 * 실제 Kafka 발행은 OutboxEventRelay가 담당한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QuizEventPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Value("${kafka.topics.quiz.submitted}")
    private String quizSubmittedTopic;

    @Value("${kafka.topics.quiz.graded}")
    private String quizGradedTopic;

    @EventListener
    public void handleQuizAnswerSubmitted(QuizAnswerSubmitted event) {
        QuizAnswerSubmittedEvent dto = new QuizAnswerSubmittedEvent(
                event.userId().value(),
                event.answeredAt()
        );
        saveToOutbox(quizSubmittedTopic, event.userId().value().toString(), dto);
        log.info("[Outbox] QuizAnswerSubmitted 저장 - userId: {}", event.userId().value());
    }

    @EventListener
    public void handlePointQuizPassed(PointQuizPassed event) {
        PointQuizPassedEvent dto = new PointQuizPassedEvent(
                event.userId().value(),
                event.seedMoney().value(),
                event.endedAt()
        );
        saveToOutbox(quizGradedTopic, event.userId().value().toString(), dto);
        log.info("[Outbox] PointQuizPassed 저장 - userId: {}", event.userId().value());
    }

    private void saveToOutbox(String topic, String aggregateId, Object payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            outboxEventRepository.save(OutboxEvent.create(topic, aggregateId, json));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Outbox 이벤트 직렬화 실패", e);
        }
    }
}
