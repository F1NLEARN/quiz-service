package com.finlearn.quizservice.quizetl.presentation.scheduler;

import com.finlearn.quizservice.quizetl.infrastructure.kafka.producer.QuizETLProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class QuizETLScheduler {

    private final QuizETLProducer quizETLProducer;

    // 매일 새벽 2시 전체 ETL 파이프라인 트리거 (Kafka를 통한 비동기 Saga 시작)
    @Scheduled(cron = "0 0 2 * * *")
    public void generateNewKeywords() {
        log.info("[키워드 Scheduler] 새벽 2시 전체 ETL 파이프라인 트리거 시작");
        try {
            // 카프카 이벤트를 발행하여 비동기 Saga 흐름(키워드생성 -> 크롤링 -> 임베딩 -> 퀴즈생성)을 순차 구동합니다.
            quizETLProducer.triggerEtl(null);
            log.info("[키워드 Scheduler] ETL 파이프라인 트리거 성공");
        } catch (Exception e) {
            log.error("[키워드 Scheduler] ETL 파이프라인 트리거 중 오류 발생: {}", e.getMessage());
        }
    }

    // etl 단계별로 완료 시 kafka 이벤트가 발행되고 수신하여 첫 단계(새벽 2시)만 실행하면 되므로 아래 스케줄러들은 주석 처리합니다.
    //
    // // 매일 새벽 3시 위키피디아 크롤링
    // @Scheduled(cron = "0 0 3 * * *")
    // public void crawlWikipediaArticles() {
    //     log.info("[크롤링 Scheduler] 위키피디아 크롤링 시작");
    //     try {
    //         wikiCrawlerService.crawlPendingTopics(null, null, null);
    //         log.info("[크롤링 Scheduler] 위키피디아 크롤링 오류 없이 성공");
    //     } catch (Exception e) {
    //         log.error("[크롤링 Scheduler] 위키피디아 크롤링 중 오류 발생: {}", e.getMessage());
    //     }
    // }
    //
    // // 매일 새벽 4시 벡터화 및 임베딩
    // @Scheduled(cron = "0 0 4 * * *")
    // public void vectorizeTopics() {
    //     log.info("[벡터화 Scheduler] 벡터화 및 임베딩 시작");
    //     try {
    //         quizVectorService.vectorizeCrawledTopics(null, null, null);
    //         log.info("[벡터화 Scheduler] 벡터화 및 임베딩 오류 없이 성공");
    //     } catch (Exception e) {
    //         log.error("[벡터화 Scheduler] 벡터화 및 임베딩 중 오류 발생: {}", e.getMessage());
    //     }
    // }
    //
    // // 매일 새벽 5시 퀴즈 생성
    // @Scheduled(cron = "0 0 5 * * *")
    // public void generateQuizzes() {
    //     log.info("[퀴즈 생성 Scheduler] 퀴즈 생성 시작");
    //     try {
    //         quizGeneratorService.generateQuizzesFromEmbeddedTopics(null, null, null);
    //         log.info("[퀴즈 생성 Scheduler] 벡터화 및 퀴즈 생성 오류 없이 성공");
    //     } catch (Exception e) {
    //         log.error("[퀴즈 생성 Scheduler] 벡터화 및 퀴즈 생성 중 오류 발생: {}", e.getMessage());
    //     }
    // }
}
