package com.finlearn.quizservice.quizetl.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finlearn.common.exception.InternalServerException;
import com.finlearn.quizservice.quizetl.application.port.out.WikipediaPort;
import com.finlearn.quizservice.quizetl.domain.entity.CrawledSource;
import com.finlearn.quizservice.quizetl.domain.entity.QuizTopic;
import com.finlearn.quizservice.quizetl.domain.enums.TopicStatus;
import com.finlearn.quizservice.quizetl.domain.repository.CrawledSourceRepository;
import com.finlearn.quizservice.quizetl.domain.repository.QuizTopicRepository;
import com.finlearn.quizservice.quizetl.infrastructure.kafka.event.QuizCrawledEvent;
import com.finlearn.quizservice.quizsession.infrastructure.outbox.OutboxEvent;
import com.finlearn.quizservice.quizsession.infrastructure.outbox.OutboxEventRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class WikiCrawlerService {

    private final QuizTopicRepository quizTopicRepository;
    private final CrawledSourceRepository crawledSourceRepository;
    private final WikipediaPort wikipediaPort;
    private final TransactionTemplate transactionTemplate;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Value("${kafka.topics.quiz.crawled:finlearn-quiz-crawled}")
    private String topicCrawled;

    public String fetchArticleContent(String keyword) {
        return wikipediaPort.fetchArticleContent(keyword);
    }

    public void crawlPendingTopics(UUID userId, String email, String role) {
        List<QuizTopic> pendingTopics = quizTopicRepository.findByStatus(TopicStatus.PENDING);
        log.info("크롤링 대기 중인 토픽 {}개", pendingTopics.size());

        int successCount = 0;

        for (QuizTopic topic : pendingTopics) {
            String keyword = topic.getSubTopic();
            String sourceUrl = wikipediaPort.generateArticleUrl(keyword);

            // 이미 크롤링된 출처인지 확인
            Boolean alreadyExists = transactionTemplate.execute(status -> {
                if (crawledSourceRepository.existsBySourceUrl(sourceUrl)) {
                    topic.updateStatus(TopicStatus.CRAWLED);
                    quizTopicRepository.save(topic);
                    return true;
                }
                return false;
            });

            if (Boolean.TRUE.equals(alreadyExists)) {
                log.info("'{}' 는 이미 수집된 출처라 상태를 CRAWLED로 업데이트하고 스킵", keyword);
                successCount++;
                continue;
            }

            try {
                // 크롤링
                String content = fetchArticleContent(keyword);

                if (content != null && !content.trim().isEmpty()) {
                    // 크롤링된 데이터 저장 및 quizTopic 상태 변경
                    transactionTemplate.executeWithoutResult(status -> {
                        // 크롤링 오래걸려서 나중을 대비해 이중 검증
                        if (!crawledSourceRepository.existsBySourceUrl(sourceUrl)) {
                            CrawledSource source = CrawledSource.builder().sourceUrl(sourceUrl).keyword(keyword)
                                    .title(keyword).content(content).rawResponse(content).build();
                            crawledSourceRepository.save(source);
                        }
                        topic.updateStatus(TopicStatus.CRAWLED);
                        quizTopicRepository.save(topic);
                    });
                    log.info("'{}' 크롤링 성공 및 저장 완료", keyword);
                    successCount++;
                } else {
                    transactionTemplate.executeWithoutResult(status -> {
                        topic.updateStatus(TopicStatus.NOT_FOUND);
                        quizTopicRepository.save(topic);
                    });
                    log.warn("'{}' 크롤링 실패 (문서 없음)", keyword);
                }

                // 위키피디아 API Rate Limit 보호를 위해서 sleep
                Thread.sleep(1000);
            } catch (Exception e) {
                log.error("'{}' 크롤링 중 오류 발생 (일시적 오류로 간주하여 재시도): {}", keyword, e.getMessage());
                throw new RuntimeException("크롤링 인프라 오류로 인해 해당 배치를 중단하고 재시도합니다.", e);
            }
        }

        if (successCount > 0) {
            log.info("크롤링 배치 끝. 성공한 토픽이 있으므로 crawled 이벤트 outbox에 저장 시작");

            transactionTemplate.executeWithoutResult(status -> {
                QuizCrawledEvent event = new QuizCrawledEvent(userId, email, role, LocalDateTime.now());

                try {
                    String json = objectMapper.writeValueAsString(event);
                    outboxEventRepository.save(
                            OutboxEvent.create(topicCrawled, userId != null ? userId.toString() : "SYSTEM", json));
                    log.info("crawled 이벤트 outbox에 저장 완료");
                } catch (JsonProcessingException e) {
                    throw new InternalServerException("이벤트 직렬화 실패: " + e.getMessage());
                }
            });
        } else {
            log.info("크롤링 배치 끝. 성공한 토픽이 없어 이벤트를 발행하지 않습니다.");
        }
    }
}
