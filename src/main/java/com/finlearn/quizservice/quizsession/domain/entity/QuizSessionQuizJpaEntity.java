package com.finlearn.quizservice.quizsession.domain.entity;

import com.finlearn.common.domain.BaseEntity;
import com.finlearn.quizservice.quizsession.domain.QuizSessionQuiz;
import com.finlearn.quizservice.quizsession.domain.vo.QuizId;
import com.finlearn.quizservice.quizsession.domain.vo.QuizSessionQuizId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * quiz_session_quizzes 테이블과 매핑되는 JPA 엔티티.
 * QuizSessionJpaEntity의 자식으로, 세션에 포함된 개별 문제를 나타낸다.
 */
@Entity
@Table(name = "quiz_session_quizzes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuizSessionQuizJpaEntity extends BaseEntity {

    @Id
    @Column(name = "quiz_session_quizzes_id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_sessions_id", nullable = false)
    private QuizSessionJpaEntity session;

    @Column(name = "quizzes_id", nullable = false)
    private UUID quizId;

    @Column(name = "order_no", nullable = false)
    private int orderNo;

    /** 사용자 제출 답안 선택지 번호. 미풀이 시 NULL */
    @Column(name = "submitted")
    private Integer submitted;

    @Column(name = "concept_included", nullable = false)
    private boolean conceptIncluded;

    /** 정오답 여부. 미풀이 시 NULL */
    @Column(name = "correct")
    private Boolean correct;

    /** 답안 제출 시각. 미풀이 시 NULL */
    @Column(name = "answered_at", columnDefinition = "timestamptz")
    private OffsetDateTime answeredAt;

    /**
     * 도메인 객체에서 JPA 엔티티를 생성한다.
     * 부모 엔티티 참조를 함께 설정하여 양방향 연관 관계를 완성한다.
     */
    public static QuizSessionQuizJpaEntity fromDomain(QuizSessionQuiz quiz,
                                                       QuizSessionJpaEntity sessionEntity) {
        QuizSessionQuizJpaEntity entity = new QuizSessionQuizJpaEntity();
        // 식별자 및 기본 정보 매핑
        entity.id = quiz.getId().value();
        entity.session = sessionEntity;
        entity.quizId = quiz.getQuizId().value();
        entity.orderNo = quiz.getOrderNo();
        entity.submitted = quiz.getSubmitted();
        entity.conceptIncluded = quiz.isConceptIncluded();
        entity.correct = quiz.getCorrect();
        entity.answeredAt = quiz.getAnsweredAt();
        return entity;
    }

    /**
     * JPA 엔티티에서 도메인 객체를 재구성한다.
     */
    public QuizSessionQuiz toDomain() {
        return QuizSessionQuiz.reconstruct(
                QuizSessionQuizId.of(id),
                QuizId.of(quizId),
                orderNo,
                submitted,
                conceptIncluded,
                correct,
                answeredAt
        );
    }
}
