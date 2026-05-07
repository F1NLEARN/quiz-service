package com.finlearn.quizservice.quizsession.domain.vo;

import java.util.UUID;

/**
 * 세션 개념 정리 식별자 VO.
 */
public record SessionConceptSummaryId(UUID value) {

    public static SessionConceptSummaryId of(UUID value) {
        return new SessionConceptSummaryId(value);
    }

    public static SessionConceptSummaryId newId() {
        return new SessionConceptSummaryId(UUID.randomUUID());
    }
}
