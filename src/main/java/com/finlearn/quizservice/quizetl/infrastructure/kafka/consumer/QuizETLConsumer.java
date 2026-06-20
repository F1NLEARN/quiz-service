package com.finlearn.quizservice.quizetl.infrastructure.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finlearn.common.security.HeaderUserDetails;
import com.finlearn.quizservice.quizetl.application.service.QuizGeneratorService;
import com.finlearn.quizservice.quizetl.application.service.QuizTopicGeneratorService;
import com.finlearn.quizservice.quizetl.application.service.QuizVectorService;
import com.finlearn.quizservice.quizetl.application.service.WikiCrawlerService;
import com.finlearn.quizservice.quizetl.infrastructure.kafka.event.QuizCrawledEvent;
import com.finlearn.quizservice.quizetl.infrastructure.kafka.event.QuizETLTriggeredEvent;
import com.finlearn.quizservice.quizetl.infrastructure.kafka.event.QuizEmbeddedEvent;
import com.finlearn.quizservice.quizetl.infrastructure.kafka.event.QuizTopicCreatedEvent;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.retry.annotation.Backoff;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class QuizETLConsumer {

    private final QuizTopicGeneratorService quizTopicGeneratorService;
    private final WikiCrawlerService wikiCrawlerService;
    private final QuizVectorService quizVectorService;
    private final QuizGeneratorService quizGeneratorService;
    private final ObjectMapper objectMapper;

    // ETL 트리거 수신 -> 키워드 생성 시작
    @RetryableTopic(attempts = "3", backoff = @Backoff(delay = 600000), autoCreateTopics = "true")
    @KafkaListener(topics = "${kafka.topics.quiz.etl-triggered:finlearn-quiz-etl-triggered}", groupId = "${spring.kafka.consumer.group-id:quiz-service}", properties = {
            "max.poll.interval.ms:3600000" })
    public void consumeEtlTrigger(ConsumerRecord<String, Object> record, Acknowledgment acknowledgment) {
        QuizETLTriggeredEvent event = convertEvent(record, QuizETLTriggeredEvent.class);
        log.info("[Saga-Step1] ETL 트리거 수신 - count: {}", event.getCount());

        runWithSecurityContext(event.getUserId(), event.getEmail(), event.getRole(), () -> {
            if (event.getCount() == null) {
                quizTopicGeneratorService.generateAllTopicsKeywords(event.getUserId(), event.getEmail(),
                        event.getRole());
            } else {
                quizTopicGeneratorService.generateAllTopicsKeywords(event.getCount(), event.getUserId(),
                        event.getEmail(), event.getRole());
            }
        });
        acknowledgment.acknowledge();
    }

    // 키워드 생성 완료 수신 -> 위키 크롤링 시작
    @RetryableTopic(attempts = "3", backoff = @Backoff(delay = 600000), autoCreateTopics = "true")
    @KafkaListener(topics = "${kafka.topics.quiz.topic-created:finlearn-quiz-topic-created}", groupId = "${spring.kafka.consumer.group-id:quiz-service}", properties = {
            "max.poll.interval.ms:3600000" })
    public void consumeTopicCreated(ConsumerRecord<String, Object> record, Acknowledgment acknowledgment) {
        QuizTopicCreatedEvent event = convertEvent(record, QuizTopicCreatedEvent.class);
        log.info("키워드 생성 완료 수신 -> 크롤링 시작");

        runWithSecurityContext(event.getUserId(), event.getEmail(), event.getRole(), () -> {
            wikiCrawlerService.crawlPendingTopics(event.getUserId(), event.getEmail(), event.getRole());
        });
        acknowledgment.acknowledge();
    }

    // 크롤링 완료 수신 -> 벡터화(임베딩) 시작
    @RetryableTopic(attempts = "3", backoff = @Backoff(delay = 600000), autoCreateTopics = "true")
    @KafkaListener(topics = "${kafka.topics.quiz.crawled:finlearn-quiz-crawled}", groupId = "${spring.kafka.consumer.group-id:quiz-service}", properties = {
            "max.poll.interval.ms:3600000" })
    public void consumeCrawled(ConsumerRecord<String, Object> record, Acknowledgment acknowledgment) {
        QuizCrawledEvent event = convertEvent(record, QuizCrawledEvent.class);
        log.info("크롤링 완료 수신 -> 벡터화 시작");

        runWithSecurityContext(event.getUserId(), event.getEmail(), event.getRole(), () -> {
            quizVectorService.vectorizeCrawledTopics(event.getUserId(), event.getEmail(), event.getRole());
        });
        acknowledgment.acknowledge();
    }

    // 벡터화 완료 수신 -> 퀴즈 생성 시작
    @RetryableTopic(attempts = "3", backoff = @Backoff(delay = 600000), autoCreateTopics = "true")
    @KafkaListener(topics = "${kafka.topics.quiz.embedded:finlearn-quiz-embedded}", groupId = "${spring.kafka.consumer.group-id:quiz-service}", properties = {
            "max.poll.interval.ms:3600000" })
    public void consumeEmbedded(ConsumerRecord<String, Object> record, Acknowledgment acknowledgment) {
        QuizEmbeddedEvent event = convertEvent(record, QuizEmbeddedEvent.class);
        log.info("벡터화 완료 수신 -> 퀴즈 생성 시작");

        runWithSecurityContext(event.getUserId(), event.getEmail(), event.getRole(), () -> {
            quizGeneratorService.generateQuizzesFromEmbeddedTopics(event.getUserId(), event.getEmail(),
                    event.getRole());
        });
        acknowledgment.acknowledge();
    }

    private <T> T convertEvent(ConsumerRecord<String, Object> record, Class<T> clazz) {
        return objectMapper.convertValue(record.value(), clazz);
    }

    private void runWithSecurityContext(UUID userId, String email, String role, Runnable runnable) {
        if (userId != null && role != null) {
            HeaderUserDetails userDetails = new HeaderUserDetails(userId, email, role);
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(userDetails, null,
                    userDetails.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        try {
            runnable.run();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
