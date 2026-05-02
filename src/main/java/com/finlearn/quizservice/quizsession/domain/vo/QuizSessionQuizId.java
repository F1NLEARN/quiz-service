package com.finlearn.quizservice.quizsession.domain.vo;

import java.util.UUID;

/**
 * 세션-문제 연결 엔티티 식별자 VO.
 * ChatConversation이 QuizSessionQuiz를 ID로 참조할 때 사용된다.
 */
public record QuizSessionQuizId(UUID value) {

    public static QuizSessionQuizId of(UUID value) {
        return new QuizSessionQuizId(value);
    }

    public static QuizSessionQuizId newId() {
        return new QuizSessionQuizId(UUID.randomUUID());
    }
}
