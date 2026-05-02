package com.finlearn.quizservice.quizsession.domain.vo;

import java.util.UUID;

/**
 * 퀴즈 세션 식별자 VO.
 * UUID를 래핑해 타입 안전성을 제공하고 Aggregate 간 ID 참조에 사용된다.
 */
public record QuizSessionId(UUID value) {

    public static QuizSessionId of(UUID value) {
        return new QuizSessionId(value);
    }

    public static QuizSessionId newId() {
        return new QuizSessionId(UUID.randomUUID());
    }
}
