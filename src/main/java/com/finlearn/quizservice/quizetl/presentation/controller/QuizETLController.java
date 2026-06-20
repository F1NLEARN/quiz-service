package com.finlearn.quizservice.quizetl.presentation.controller;

import com.finlearn.common.response.CommonResponse;
import com.finlearn.quizservice.quizetl.infrastructure.kafka.producer.QuizETLProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// TODO: role이 ADMIN인 유저만 수동 실행이 가능하도록 처리 예정
@Slf4j
@RestController
@RequestMapping("/api/v1/quiz/etl")
@RequiredArgsConstructor
public class QuizETLController {

    private final QuizETLProducer quizETLProducer;

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public CommonResponse<String> runFullPipeline(@RequestParam(required = false) Integer count) {
        log.info("전체 ETL 파이프라인 수동 실행");
        quizETLProducer.triggerEtl(count);
        return CommonResponse.success("전체 ETL 파이프라인 수동 실행 성공", null);
    }

    @PostMapping("/resume/vectorization")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public CommonResponse<String> resumeVectorizationPipeline() {
        log.info("크롤링 완료 kafka 이벤트 수동 발행-임베딩 실행");
        quizETLProducer.triggerCrawledResume();
        return CommonResponse.success("벡터화 파이프라인 수동 재개 이벤트 발행 성공", null);
    }

    @PostMapping("/resume/generation")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public CommonResponse<String> resumeGenerationPipeline() {
        log.info("임베딩 완료 kafka 이벤트 수동 발행-퀴즈생성 실행");
        quizETLProducer.triggerEmbeddedResume();
        return CommonResponse.success("퀴즈 생성 파이프라인 수동 재개 이벤트 발행 성공", null);
    }
}
