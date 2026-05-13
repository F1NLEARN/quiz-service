package com.finlearn.quizservice.quizsession.infrastructure.repository;

import com.finlearn.quizservice.quizsession.domain.entity.SessionConceptSummaryJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SessionConceptSummaryJpaRepository extends JpaRepository<SessionConceptSummaryJpaEntity, UUID> {

    /** 세션 ID로 개념 정리를 조회한다 */
    Optional<SessionConceptSummaryJpaEntity> findByQuizSessionId(UUID quizSessionId);
}
