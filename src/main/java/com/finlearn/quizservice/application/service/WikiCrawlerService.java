package com.finlearn.quizservice.application.service;

import com.finlearn.quizservice.domain.entity.CrawledSource;
import com.finlearn.quizservice.domain.entity.QuizTopic;
import com.finlearn.quizservice.domain.enums.TopicStatus;
import com.finlearn.quizservice.domain.repository.CrawledSourceRepository;
import com.finlearn.quizservice.domain.repository.QuizTopicRepository;
import com.finlearn.quizservice.infrastructure.client.WikipediaClient;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class WikiCrawlerService {

    private static final String WIKI_WEB_URL_PREFIX = "https://ko.wikipedia.org/wiki/";

    private final QuizTopicRepository quizTopicRepository;
    private final CrawledSourceRepository crawledSourceRepository;
    private final WikipediaClient wikipediaClient;

    public String fetchArticleContent(String keyword) {
        return wikipediaClient.fetchArticleContent(keyword);
    }

    public void crawlPendingTopics() {
        List<QuizTopic> pendingTopics = quizTopicRepository.findByStatus(TopicStatus.PENDING);
        log.info("크롤링 대기 중인 토픽 {}개", pendingTopics.size());

        for (QuizTopic topic : pendingTopics) {
            String keyword = topic.getSubTopic();
            String sourceUrl = WIKI_WEB_URL_PREFIX + keyword.replace(" ", "_");

            // 이미 크롤링된 출처인지 확인
            if (crawledSourceRepository.existsBySourceUrl(sourceUrl)) {
                log.info("'{}' 는 이미 수집된 출처라 상태를 CRAWLED로 업데이트", keyword);
                topic.updateStatus(TopicStatus.CRAWLED);
                quizTopicRepository.save(topic);
                continue;
            }

            try {
                String content = fetchArticleContent(keyword);

                if (content != null && !content.trim().isEmpty()) {
                    CrawledSource source = CrawledSource.builder().sourceUrl(sourceUrl).keyword(keyword).title(keyword)
                            .content(content).rawResponse(content).build();

                    crawledSourceRepository.save(source);
                    topic.updateStatus(TopicStatus.CRAWLED);
                    log.info("'{}' 크롤링 성공 및 저장 완료", keyword);
                } else {
                    topic.updateStatus(TopicStatus.NOT_FOUND);
                    log.warn("'{}' 크롤링 실패 (문서 없음)", keyword);
                }

                quizTopicRepository.save(topic);

                // 위키피디아 API Rate Limit 보호를 위해서 sleep
                Thread.sleep(1000);
            } catch (Exception e) {
                log.error("'{}' 크롤링 중 오류 발생: {}", keyword, e.getMessage());
            }
        }

        log.info("크롤링 배치 끝");
    }
}
