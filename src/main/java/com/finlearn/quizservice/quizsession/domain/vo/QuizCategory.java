package com.finlearn.quizservice.quizsession.domain.vo;

/**
 * 학습 퀴즈 카테고리 VO.
 * String 기반으로 quiz content 도메인의 카테고리 목록과 느슨하게 결합한다.
 * 포인트 퀴즈 세션에서는 사용되지 않는다(null).
 */
public record QuizCategory(String value) {

    public QuizCategory {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("카테고리 값은 비어있을 수 없습니다.");
        }
    }

    public static QuizCategory of(String value) {
        return new QuizCategory(value);
    }
}
