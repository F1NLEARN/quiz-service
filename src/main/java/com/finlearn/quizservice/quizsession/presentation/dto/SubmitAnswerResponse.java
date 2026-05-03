package com.finlearn.quizservice.quizsession.presentation.dto;

import java.util.UUID;

/**
 * 답안 제출 응답 DTO.
 * 포인트 퀴즈는 answerExplanation이 null로 반환된다.
 * 학습 퀴즈는 answerExplanation을 즉시 포함한다.
 */
public record SubmitAnswerResponse(
        UUID quizId,
        int submitted,
        boolean correct,
        int correctNo,
        String answerExplanation
) {

    public static SubmitAnswerResponse of(UUID quizId, int submitted, boolean correct,
                                          int correctNo, String answerExplanation) {
        return new SubmitAnswerResponse(quizId, submitted, correct, correctNo, answerExplanation);
    }
}
