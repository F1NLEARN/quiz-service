package com.finlearn.quizservice.quizsession.presentation.dto;

import com.finlearn.quizservice.domain.entity.QuizChoice;

/**
 * 선택지 응답 DTO.
 * isCorrect 필드를 의도적으로 제외하여 정답을 노출하지 않는다.
 */
public record ChoiceResponse(int no, String content) {

    public static ChoiceResponse from(QuizChoice choice) {
        return new ChoiceResponse(choice.getNo(), choice.getContent());
    }
}
