package com.finlearn.quizservice.quizsession.infrastructure.repository;

import com.finlearn.quizservice.quizsession.domain.SessionConceptSummary;
import com.finlearn.quizservice.quizsession.domain.entity.SessionConceptSummaryJpaEntity;
import com.finlearn.quizservice.quizsession.domain.repository.SessionConceptSummaryRepository;
import com.finlearn.quizservice.quizsession.domain.vo.QuizSessionId;
import com.finlearn.quizservice.quizsession.domain.vo.SessionConceptSummaryId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * SessionConceptSummaryRepository 도메인 인터페이스의 JPA 구현체.
 */
@Repository
@RequiredArgsConstructor
public class SessionConceptSummaryRepositoryImpl implements SessionConceptSummaryRepository {

    private final SessionConceptSummaryJpaRepository jpaRepository;

    @Override
    public SessionConceptSummary save(SessionConceptSummary summary) {
        return jpaRepository.save(SessionConceptSummaryJpaEntity.fromDomain(summary)).toDomain();
    }

    @Override
    public Optional<SessionConceptSummary> findById(SessionConceptSummaryId id) {
        return jpaRepository.findById(id.value())
                .map(SessionConceptSummaryJpaEntity::toDomain);
    }

    @Override
    public Optional<SessionConceptSummary> findByQuizSessionId(QuizSessionId quizSessionId) {
        return jpaRepository.findByQuizSessionId(quizSessionId.value())
                .map(SessionConceptSummaryJpaEntity::toDomain);
    }
}
