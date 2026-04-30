package com.finlearn.quizservice.presentation.scheduler;

import com.finlearn.quizservice.application.service.QuizTopicGeneratorService;
import com.finlearn.quizservice.application.service.WikiCrawlerService;
import com.finlearn.quizservice.domain.enums.MainTopic;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class QuizETLScheduler {

    private static final int TOPIC_COUNT_PER_BATCH = 10;

    private final QuizTopicGeneratorService quizTopicGeneratorService;
    private final WikiCrawlerService wikiCrawlerService;

    // 새로운 퀴즈 키워드 생성
    @Scheduled(cron = "0 0 2 * * SUN") // 매주 일요일 새벽 2시
    // @Scheduled(fixedDelay = 604800000)
    public void generateNewKeywords() {
        log.info("[키워드 Scheduler] 새로운 퀴즈 키워드 생성 시작");
        try {
            for (MainTopic topic : MainTopic.values()) {
                log.info("[키워드 Scheduler] '{}' 대주제를 가지고 소주제 생성", topic);
                quizTopicGeneratorService.generateAndSaveTopics(topic, TOPIC_COUNT_PER_BATCH);
            }
            log.info("[키워드 Scheduler] 키워드 생성 오류 없이 성공");
        } catch (Exception e) {
            log.error("[키워드 Scheduler] 키워드 생성 중 오류 발생: {}", e.getMessage());
        }
    }

    // 위키피디아 크롤링
    @Scheduled(cron = "0 0 3 * * *") // 매일 새벽 3시
    // @Scheduled(fixedDelay = 86400000)
    public void crawlWikipediaArticles() {
        log.info("[크롤링 Scheduler] 위키피디아 크롤링 시작");
        try {
            wikiCrawlerService.crawlPendingTopics();
            log.info("[크롤링 Scheduler] 위키피디아 크롤링 오류 없이 성공");
        } catch (Exception e) {
            log.error("[크롤링 Scheduler] 위키피디아 크롤링 중 오류 발생: {}", e.getMessage());
        }
    }
}
