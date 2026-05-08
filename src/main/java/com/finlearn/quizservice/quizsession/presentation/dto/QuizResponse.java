package com.finlearn.quizservice.quizsession.presentation.dto;

import com.finlearn.quizservice.quizetl.domain.entity.Quiz;
import com.finlearn.quizservice.quizsession.domain.QuizSessionQuiz;

import java.util.List;
import java.util.UUID;

/**
 * 문제 조회 응답 DTO.
 * 문제 컨텐츠를 반환하되 choices의 correct는 포함하지 않는다.
 */
public record QuizResponse(
        UUID quizSessionQuizId,
        int orderNo,
        UUID quizId,
        String title,
        String question,
        List<ChoiceResponse> choices
) {

    public static QuizResponse from(QuizSessionQuiz sessionQuiz, Quiz quiz) {
        List<ChoiceResponse> choices = quiz.getChoices().stream()
                .map(ChoiceResponse::from)
                .toList();

        return new QuizResponse(
                sessionQuiz.getId().value(),
                sessionQuiz.getOrderNo(),
                quiz.getId(),
                quiz.getTitle(),
                quiz.getQuestion(),
                choices
        );
    }
}
