package com.finlearn.quizservice.quizsession.domain.repository;

import com.finlearn.quizservice.quizsession.domain.SessionConceptSummary;
import com.finlearn.quizservice.quizsession.domain.vo.QuizSessionId;
import com.finlearn.quizservice.quizsession.domain.vo.SessionConceptSummaryId;

import java.util.Optional;

/**
 * 세션 개념 정리 도메인 Repository 인터페이스.
 * Infrastructure 레이어의 JPA 구현체가 이 인터페이스를 구현한다.
 */
public interface SessionConceptSummaryRepository {

    /** 세션 개념 정리를 저장하고 저장된 객체를 반환한다 */
    SessionConceptSummary save(SessionConceptSummary summary);

    /** ID로 개념 정리를 조회한다 */
    Optional<SessionConceptSummary> findById(SessionConceptSummaryId id);

    /** 세션 ID로 개념 정리를 조회한다 */
    Optional<SessionConceptSummary> findByQuizSessionId(QuizSessionId quizSessionId);
}
