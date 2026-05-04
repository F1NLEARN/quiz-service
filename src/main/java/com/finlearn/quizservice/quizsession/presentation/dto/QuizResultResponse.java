package com.finlearn.quizservice.quizsession.presentation.dto;

import com.finlearn.quizservice.domain.entity.Quiz;
import com.finlearn.quizservice.domain.entity.QuizChoice;
import com.finlearn.quizservice.quizsession.domain.QuizSessionQuiz;

import java.util.UUID;

/**
 * 세션 종료 시 문제별 결과 응답 DTO.
 * 학습 퀴즈 종료 시에만 사용되며, 정답·해설을 포함한다.
 */
public record QuizResultResponse(
        int orderNo,
        UUID quizId,
        Integer submitted,
        boolean correct,
        int correctNo,
        String answerExplanation
) {

    public static QuizResultResponse from(QuizSessionQuiz sessionQuiz, Quiz quiz) {
        // choices에서 정답 선택지 번호 추출
        int correctNo = quiz.getChoices().stream()
                .filter(QuizChoice::isCorrect)
                .findFirst()
                .map(QuizChoice::getNo)
                .orElse(0);

        return new QuizResultResponse(
                sessionQuiz.getOrderNo(),
                quiz.getId(),
                sessionQuiz.getSubmitted(),
                Boolean.TRUE.equals(sessionQuiz.getCorrect()),
                correctNo,
                quiz.getAnswerExplanation()
        );
    }
}
