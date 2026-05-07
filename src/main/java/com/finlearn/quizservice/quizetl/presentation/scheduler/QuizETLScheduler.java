package com.finlearn.quizservice.quizetl.presentation.scheduler;

import com.finlearn.quizservice.quizetl.application.service.QuizGeneratorService;
import com.finlearn.quizservice.quizetl.application.service.QuizTopicGeneratorService;
import com.finlearn.quizservice.quizetl.application.service.QuizVectorService;
import com.finlearn.quizservice.quizetl.application.service.WikiCrawlerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class QuizETLScheduler {

    private final QuizTopicGeneratorService quizTopicGeneratorService;
    private final WikiCrawlerService wikiCrawlerService;
    private final QuizVectorService quizVectorService;
    private final QuizGeneratorService quizGeneratorService;

    // 매일 새벽 2시 새로운 퀴즈 키워드 생성
    @Scheduled(cron = "0 0 2 * * *")
    public void generateNewKeywords() {
        log.info("[키워드 Scheduler] 새로운 퀴즈 키워드 생성 시작");
        try {
            quizTopicGeneratorService.generateAllTopicsKeywords();
            log.info("[키워드 Scheduler] 키워드 생성 오류 없이 성공");
        } catch (Exception e) {
            log.error("[키워드 Scheduler] 키워드 생성 중 오류 발생: {}", e.getMessage());
        }
    }

    // 매일 새벽 3시 위키피디아 크롤링
    @Scheduled(cron = "0 0 3 * * *")
    public void crawlWikipediaArticles() {
        log.info("[크롤링 Scheduler] 위키피디아 크롤링 시작");
        try {
            wikiCrawlerService.crawlPendingTopics();
            log.info("[크롤링 Scheduler] 위키피디아 크롤링 오류 없이 성공");
        } catch (Exception e) {
            log.error("[크롤링 Scheduler] 위키피디아 크롤링 중 오류 발생: {}", e.getMessage());
        }
    }

    // 매일 새벽 4시 벡터화 및 임베딩
    @Scheduled(cron = "0 0 4 * * *")
    public void vectorizeTopics() {
        log.info("[벡터화 Scheduler] 벡터화 및 임베딩 시작");
        try {
            quizVectorService.vectorizeCrawledTopics();
            log.info("[벡터화 Scheduler] 벡터화 및 임베딩 오류 없이 성공");
        } catch (Exception e) {
            log.error("[벡터화 Scheduler] 벡터화 및 임베딩 중 오류 발생: {}", e.getMessage());
        }
    }

    // 매일 새벽 5시 퀴즈 생성
    @Scheduled(cron = "0 0 5 * * *")
    public void generateQuizzes() {
        log.info("[퀴즈 생성 Scheduler] 퀴즈 생성 시작");
        try {
            quizGeneratorService.generateQuizzesFromEmbeddedTopics();

            log.info("[퀴즈 생성 Scheduler] 벡터화 및 퀴즈 생성 오류 없이 성공");
        } catch (Exception e) {
            log.error("[퀴즈 생성 Scheduler] 벡터화 및 퀴즈 생성 중 오류 발생: {}", e.getMessage());
        }
    }
}
