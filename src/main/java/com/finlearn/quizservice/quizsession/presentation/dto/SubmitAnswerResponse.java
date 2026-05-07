package com.finlearn.quizservice.quizsession.presentation.dto;

import java.util.UUID;

/**
 * 답안 제출 응답 DTO.
 * 정오답 여부와 정답 번호만 반환한다. 해설은 챗봇을 통해 제공된다.
 */
public record SubmitAnswerResponse(
        UUID quizId,
        int submitted,
        boolean correct,
        int correctNo
) {

    public static SubmitAnswerResponse of(UUID quizId, int submitted, boolean correct, int correctNo) {
        return new SubmitAnswerResponse(quizId, submitted, correct, correctNo);
    }
}
