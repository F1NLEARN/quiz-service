package com.finlearn.quizservice.quizsession.application.service;

import com.finlearn.quizservice.quizsession.domain.SessionConceptSummary;
import com.finlearn.quizservice.quizsession.domain.exception.QuizSessionErrorCode;
import com.finlearn.quizservice.quizsession.domain.exception.QuizSessionException;
import com.finlearn.quizservice.quizsession.domain.repository.SessionConceptSummaryRepository;
import com.finlearn.quizservice.quizsession.domain.vo.QuizSessionId;
import com.finlearn.quizservice.quizsession.presentation.dto.ConceptSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 세션 개념 정리 조회 Application 서비스.
 */
@Service
@RequiredArgsConstructor
public class GetConceptSummaryService {

    private final SessionConceptSummaryRepository sessionConceptSummaryRepository;

    @Transactional(readOnly = true)
    public ConceptSummaryResponse getConceptSummary(UUID sessionId) {
        SessionConceptSummary summary = sessionConceptSummaryRepository
                .findByQuizSessionId(QuizSessionId.of(sessionId))
                .orElseThrow(() -> new QuizSessionException(QuizSessionErrorCode.SESSION_NOT_FOUND));

        return ConceptSummaryResponse.from(summary);
    }
}
