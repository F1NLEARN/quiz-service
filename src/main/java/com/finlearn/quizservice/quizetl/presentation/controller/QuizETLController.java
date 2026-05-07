package com.finlearn.quizservice.quizetl.presentation.controller;

import com.finlearn.common.response.CommonResponse;
import com.finlearn.quizservice.quizetl.application.service.QuizGeneratorService;
import com.finlearn.quizservice.quizetl.application.service.QuizTopicGeneratorService;
import com.finlearn.quizservice.quizetl.application.service.QuizVectorService;
import com.finlearn.quizservice.quizetl.application.service.WikiCrawlerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/quiz-etl")
@RequiredArgsConstructor
public class QuizETLController {

    private final QuizTopicGeneratorService quizTopicGeneratorService;
    private final WikiCrawlerService wikiCrawlerService;
    private final QuizVectorService quizVectorService;
    private final QuizGeneratorService quizGeneratorService;

    @PostMapping
    public CommonResponse<String> runFullPipeline(@RequestParam(required = false) Integer count) {
        log.info("전체 ETL 파이프라인 수동 실행");

        // 1. 키워드 생성
        if (count == null) {
            quizTopicGeneratorService.generateAllTopicsKeywords();
        } else {
            quizTopicGeneratorService.generateAllTopicsKeywords(count);
        }

        // 2. 크롤링 실행
        wikiCrawlerService.crawlPendingTopics();

        // 3. 청킹 및 임베딩
        quizVectorService.vectorizeCrawledTopics();

        // 4. 퀴즈 생성 및 품질 검증
        quizGeneratorService.generateQuizzesFromEmbeddedTopics();

        return CommonResponse.success("전체 ETL 파이프라인 수동 실행 성공", null);
    }
}
