package com.finlearn.quizservice.quizsession.presentation.dto;

import com.finlearn.quizservice.domain.entity.Quiz;
import com.finlearn.quizservice.quizsession.domain.QuizSessionQuiz;

import java.util.List;
import java.util.UUID;

/**
 * 다음 문제 조회 응답 DTO.
 * 문제 컨텐츠를 반환하되 choices의 isCorrect는 포함하지 않는다.
 */
public record NextQuizResponse(
        UUID quizSessionQuizId,
        int orderNo,
        UUID quizId,
        String title,
        String question,
        List<ChoiceResponse> choices
) {

    public static NextQuizResponse from(QuizSessionQuiz sessionQuiz, Quiz quiz) {
        // choices 변환 시 isCorrect 제거
        List<ChoiceResponse> choices = quiz.getChoices().stream()
                .map(ChoiceResponse::from)
                .toList();

        return new NextQuizResponse(
                sessionQuiz.getId().value(),
                sessionQuiz.getOrderNo(),
                quiz.getId(),
                quiz.getTitle(),
                quiz.getQuestion(),
                choices
        );
    }
}
