package com.finlearn.quizservice.quizsession.presentation.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 학습 퀴즈 개념 정리 대상 선택 요청 DTO.
 */
public record SelectConceptIncludesRequest(
        @NotNull List<Integer> orderNos
) {}
