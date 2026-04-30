package com.finlearn.quizservice.presentation.controller;

import com.finlearn.common.response.CommonResponse;
import com.finlearn.quizservice.presentation.scheduler.QuizETLScheduler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/quiz-etl")
@RequiredArgsConstructor
public class QuizETLController {

    private final QuizETLScheduler quizETLScheduler;

    @PostMapping
    public CommonResponse<String> runFullPipeline() {
        log.info("전체 ETL 파이프라인 수동 실행");

        // 1. 키워드 생성
        quizETLScheduler.generateNewKeywords();

        // 2. 크롤링 실행
        quizETLScheduler.crawlWikipediaArticles();

        // TODO: 3. 텍스트 분할(Chunking) 및 임베딩 파이프라인 실행
        // TODO: 4. 벡터 DB 기반 퀴즈 생성 로직 실행 (Dynamic RAG)
        // TODO: 5. 최종 중복 제거 및 품질 검수 로직 실행

        return CommonResponse.success("전체 ETL 파이프라인 수동 실행 성공", null);
    }
}
