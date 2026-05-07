package com.finlearn.quizservice.quizsession.presentation.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 학습 퀴즈 세션 생성 요청 DTO.
 *
 * @param category 학습할 카테고리 (MainTopic enum name, e.g. "DOMESTIC_STOCK")
 */
public record CreateLearningSessionRequest(@NotBlank String category) {
}
