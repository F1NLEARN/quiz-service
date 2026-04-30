package com.finlearn.quizservice.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum QuizGenerationStatus {
    SUCCESS("성공"), FAILED("실패");

    private final String description;
}
