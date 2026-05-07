package com.finlearn.quizservice.quizetl.application.service;

import com.finlearn.quizservice.quizetl.application.port.out.WikipediaPort;
import com.finlearn.quizservice.quizetl.domain.entity.CrawledSource;
import com.finlearn.quizservice.quizetl.domain.entity.QuizTopic;
import com.finlearn.quizservice.quizetl.domain.enums.TopicStatus;
import com.finlearn.quizservice.quizetl.domain.repository.CrawledSourceRepository;
import com.finlearn.quizservice.quizetl.domain.repository.QuizTopicRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    public String fetchArticleContent(String keyword) {
        return wikipediaPort.fetchArticleContent(keyword);
    }

    public void crawlPendingTopics() {
        List<QuizTopic> pendingTopics = quizTopicRepository.findByStatus(TopicStatus.PENDING);
        log.info("크롤링 대기 중인 토픽 {}개", pendingTopics.size());

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
                log.error("'{}' 크롤링 중 오류 발생: {}", keyword, e.getMessage());
            }
        }

        log.info("크롤링 배치 끝");
    }
}
