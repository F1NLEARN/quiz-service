package com.finlearn.quizservice.quizsession.presentation.dto;

import com.finlearn.quizservice.quizsession.domain.SessionConceptSummary;

/**
 * 개념 정리 조회 응답 DTO.
 */
public record ConceptSummaryResponse(
        String summaryContent
) {
    public static ConceptSummaryResponse from(SessionConceptSummary summary) {
        return new ConceptSummaryResponse(summary.getSummaryContent());
    }
}
