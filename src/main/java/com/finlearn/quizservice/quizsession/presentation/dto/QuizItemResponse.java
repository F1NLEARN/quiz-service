package com.finlearn.quizservice.quizsession.presentation.dto;

import com.finlearn.quizservice.quizsession.domain.QuizSessionQuiz;

import java.util.UUID;

/**
 * 세션에 포함된 개별 문제 정보 응답 DTO.
 * 프론트엔드가 문제 컨텐츠를 조회할 때 quizId를 기반으로 요청한다.
 */
public record QuizItemResponse(UUID quizSessionQuizId, UUID quizId, int orderNo) {

    public static QuizItemResponse from(QuizSessionQuiz quiz) {
        return new QuizItemResponse(
                quiz.getId().value(),
                quiz.getQuizId().value(),
                quiz.getOrderNo()
        );
    }
}
