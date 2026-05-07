package com.finlearn.quizservice.quizsession.domain.entity;

import com.finlearn.common.domain.BaseEntity;
import com.finlearn.quizservice.quizsession.domain.QuizSession;
import com.finlearn.quizservice.quizsession.domain.QuizSessionQuiz;
import com.finlearn.quizservice.quizsession.domain.vo.PassStatus;
import com.finlearn.quizservice.quizsession.domain.vo.QuizCategory;
import com.finlearn.quizservice.quizsession.domain.vo.QuizSessionId;
import com.finlearn.quizservice.quizsession.domain.vo.Score;
import com.finlearn.quizservice.quizsession.domain.vo.SeedMoney;
import com.finlearn.quizservice.quizsession.domain.vo.SessionType;
import com.finlearn.quizservice.quizsession.domain.vo.UserId;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * quiz_sessions 테이블과 매핑되는 JPA 엔티티.
 * 도메인 객체(QuizSession)와 분리되며, fromDomain/toDomain으로 변환한다.
 */
@Entity
@Table(name = "quiz_sessions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuizSessionJpaEntity extends BaseEntity {

    @Id
    @Column(name = "quiz_sessions_id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "session_type", nullable = false)
    private SessionType sessionType;

    /** 학습 퀴즈 카테고리. 포인트 퀴즈는 NULL */
    @Column(name = "category")
    private String category;

    @Column(name = "total_count", nullable = false)
    private int totalCount;

    @Column(name = "correct_count", nullable = false)
    private int correctCount;

    @Column(name = "score", nullable = false)
    private int score;

    @Enumerated(EnumType.STRING)
    @Column(name = "pass_status", nullable = false)
    private PassStatus passStatus;

    /** 포인트 퀴즈 PASS 시 지급 시드머니. PASS 이전에는 NULL */
    @Column(name = "seed_money")
    private Long seedMoney;

    @Column(name = "started_at", nullable = false, columnDefinition = "timestamptz")
    private OffsetDateTime startedAt;

    /** 세션 종료 시각. 진행 중에는 NULL */
    @Column(name = "ended_at", columnDefinition = "timestamptz")
    private OffsetDateTime endedAt;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<QuizSessionQuizJpaEntity> quizzes = new ArrayList<>();

    /**
     * 도메인 객체에서 JPA 엔티티를 생성한다.
     * 세션의 자식(QuizSessionQuiz) 목록도 함께 변환하여 연관 관계를 설정한다.
     */
    public static QuizSessionJpaEntity fromDomain(QuizSession session) {
        QuizSessionJpaEntity entity = new QuizSessionJpaEntity();
        // 식별자 및 기본 정보 매핑
        entity.id = session.getId().value();
        entity.userId = session.getUserId().value();
        entity.sessionType = session.getSessionType();
        entity.category = session.getCategory() != null ? session.getCategory().value() : null;
        entity.totalCount = session.getTotalCount();
        entity.correctCount = session.getCorrectCount();
        entity.score = session.getScore().value();
        entity.passStatus = session.getPassStatus();
        entity.seedMoney = session.getSeedMoney() != null ? session.getSeedMoney().value() : null;
        entity.startedAt = session.getStartedAt();
        entity.endedAt = session.getEndedAt();
        // 자식 엔티티 변환 및 양방향 연관 관계 설정
        entity.quizzes.clear();
        session.getQuizzes().stream()
                .map(q -> QuizSessionQuizJpaEntity.fromDomain(q, entity))
                .forEach(entity.quizzes::add);
        return entity;
    }

    /**
     * JPA 엔티티에서 도메인 객체를 재구성한다.
     * Repository 구현체에서 DB 조회 후 호출한다.
     */
    public QuizSession toDomain() {
        // 자식 엔티티 → 도메인 자식 객체 변환
        List<QuizSessionQuiz> domainQuizzes = quizzes.stream()
                .map(QuizSessionQuizJpaEntity::toDomain)
                .toList();
        return QuizSession.reconstruct(
                QuizSessionId.of(id),
                UserId.of(userId),
                sessionType,
                category != null ? QuizCategory.of(category) : null,
                totalCount,
                correctCount,
                Score.ofValue(score),
                passStatus,
                seedMoney != null ? SeedMoney.of(seedMoney) : null,
                startedAt,
                endedAt,
                domainQuizzes
        );
    }
}
