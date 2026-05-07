package com.finlearn.quizservice.quizsession.domain.vo;

import java.util.UUID;

/**
 * 사용자 식별자 VO.
 * Gateway가 전달하는 X-User-Id 헤더 값을 래핑한다.
 */
public record UserId(UUID value) {

    public static UserId of(UUID value) {
        return new UserId(value);
    }
}
