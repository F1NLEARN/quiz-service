package com.finlearn.quizservice.quizsession.infrastructure.repository;

import com.finlearn.quizservice.quizsession.domain.QuizSession;
import com.finlearn.quizservice.quizsession.domain.entity.QuizSessionJpaEntity;
import com.finlearn.quizservice.quizsession.domain.repository.QuizSessionRepository;
import com.finlearn.quizservice.quizsession.domain.vo.QuizSessionId;
import com.finlearn.quizservice.quizsession.domain.vo.UserId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.Optional;

/**
 * QuizSessionRepository 도메인 인터페이스의 JPA 구현체.
 * 도메인 객체 ↔ JPA 엔티티 변환을 담당하며, Application 레이어에 주입된다.
 */
@Repository
@RequiredArgsConstructor
public class QuizSessionRepositoryImpl implements QuizSessionRepository {

    private final QuizSessionJpaRepository jpaRepository;

    @Override
    public QuizSession save(QuizSession session) {
        // 도메인 → JPA 엔티티 변환 후 저장
        QuizSessionJpaEntity entity = QuizSessionJpaEntity.fromDomain(session);
        QuizSessionJpaEntity saved = jpaRepository.save(entity);
        // 저장된 엔티티 → 도메인 객체 재구성 후 반환
        return saved.toDomain();
    }

    @Override
    public Optional<QuizSession> findById(QuizSessionId id) {
        return jpaRepository.findById(id.value())
                .map(QuizSessionJpaEntity::toDomain);
    }

    @Override
    public boolean existsCompletedPointQuizInMonth(UserId userId, YearMonth yearMonth) {
        // 검사 범위: 해당 월 1일 00:00:00 UTC ~ 다음 월 1일 00:00:00 UTC
        OffsetDateTime rangeStart = yearMonth.atDay(1).atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime rangeEnd = yearMonth.plusMonths(1).atDay(1).atStartOfDay().atOffset(ZoneOffset.UTC);
        return jpaRepository.existsCompletedPointQuizInRange(userId.value(), rangeStart, rangeEnd);
    }
}
