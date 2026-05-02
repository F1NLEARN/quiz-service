package com.finlearn.quizservice.quizsession.domain.vo;

import java.util.UUID;

/**
 * 퀴즈 컨텐츠 식별자 VO.
 * 팀원 담당 quizzes 테이블의 PK를 참조하는 ID다.
 * session 도메인은 quiz 컨텐츠를 직접 참조하지 않고 이 VO로만 식별한다.
 */
public record QuizId(UUID value) {

    public static QuizId of(UUID value) {
        return new QuizId(value);
    }
}
