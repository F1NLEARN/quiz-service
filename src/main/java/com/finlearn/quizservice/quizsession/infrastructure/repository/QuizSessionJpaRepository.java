package com.finlearn.quizservice.quizsession.infrastructure.repository;

import com.finlearn.quizservice.quizsession.domain.entity.QuizSessionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * quiz_sessions 테이블에 대한 Spring Data JPA Repository.
 * 도메인 Repository 인터페이스는 QuizSessionRepositoryImpl이 구현한다.
 */
public interface QuizSessionJpaRepository extends JpaRepository<QuizSessionJpaEntity, UUID> {

    /**
     * 해당 월에 완료된(PASS 또는 FAIL) 포인트 퀴즈 세션이 있는지 확인한다.
     * 포인트 퀴즈 월 1회 제한 검증에 사용된다.
     *
     * @param userId     사용자 UUID
     * @param rangeStart 검사 시작 시각 (해당 월 1일 00:00:00)
     * @param rangeEnd   검사 종료 시각 (다음 월 1일 00:00:00, exclusive)
     */
    @Query("""
            SELECT COUNT(s) > 0
            FROM QuizSessionJpaEntity s
            WHERE s.userId = :userId
              AND s.sessionType = 'POINT'
              AND s.passStatus IN ('PASS', 'FAIL')
              AND s.startedAt >= :rangeStart
              AND s.startedAt < :rangeEnd
            """)
    boolean existsCompletedPointQuizInRange(
            @Param("userId") UUID userId,
            @Param("rangeStart") OffsetDateTime rangeStart,
            @Param("rangeEnd") OffsetDateTime rangeEnd
    );
}
