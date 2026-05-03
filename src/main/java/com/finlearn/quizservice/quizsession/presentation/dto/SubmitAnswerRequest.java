package com.finlearn.quizservice.quizsession.presentation.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 답안 제출 요청 DTO.
 *
 * @param submitted 사용자가 선택한 선택지 번호 (1~4, 4지선다 고정)
 */
public record SubmitAnswerRequest(@NotNull @Min(1) @Max(4) Integer submitted) {
}
