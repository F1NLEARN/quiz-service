package com.finlearn.quizservice.quizsession.domain.entity;

import com.finlearn.common.domain.BaseEntity;
import com.finlearn.quizservice.quizsession.domain.SessionConceptSummary;
import com.finlearn.quizservice.quizsession.domain.vo.QuizSessionId;
import com.finlearn.quizservice.quizsession.domain.vo.SessionConceptSummaryId;
import com.finlearn.quizservice.quizsession.domain.vo.UserId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * session_concept_summaries 테이블과 매핑되는 JPA 엔티티.
 * 세션 종료 시 @Async로 생성되는 개념 정리 텍스트를 저장한다.
 */
@Entity
@Table(name = "session_concept_summaries")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SessionConceptSummaryJpaEntity extends BaseEntity {

    @Id
    @Column(name = "session_concept_summaries_id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "quiz_sessions_id", nullable = false)
    private UUID quizSessionId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "summary_content", nullable = false, columnDefinition = "TEXT")
    private String summaryContent;

    /**
     * 도메인 객체에서 JPA 엔티티를 생성한다.
     */
    public static SessionConceptSummaryJpaEntity fromDomain(SessionConceptSummary summary) {
        SessionConceptSummaryJpaEntity entity = new SessionConceptSummaryJpaEntity();
        entity.id = summary.getId().value();
        entity.quizSessionId = summary.getQuizSessionId().value();
        entity.userId = summary.getUserId().value();
        entity.summaryContent = summary.getSummaryContent();
        return entity;
    }

    /**
     * JPA 엔티티에서 도메인 객체를 재구성한다.
     */
    public SessionConceptSummary toDomain() {
        return SessionConceptSummary.reconstruct(
                SessionConceptSummaryId.of(id),
                QuizSessionId.of(quizSessionId),
                UserId.of(userId),
                summaryContent
        );
    }
}
