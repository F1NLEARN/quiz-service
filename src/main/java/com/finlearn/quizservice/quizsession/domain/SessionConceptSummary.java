package com.finlearn.quizservice.quizsession.domain;

import com.finlearn.quizservice.quizsession.domain.vo.QuizSessionId;
import com.finlearn.quizservice.quizsession.domain.vo.SessionConceptSummaryId;
import com.finlearn.quizservice.quizsession.domain.vo.UserId;

/**
 * 세션 개념 정리 Aggregate Root.
 * 세션 종료 시 is_concept_included = true인 문제가 1개 이상이면
 * @Async 비동기로 Claude API를 호출해 생성된다.
 * 단독 Aggregate이며 자식 엔티티가 없다.
 * 생성 실패 시에도 세션 종료 처리에는 영향을 주지 않는다.
 */
public class SessionConceptSummary {

    private final SessionConceptSummaryId id;

    /** Aggregate 간 직접 객체 참조 금지 — ID로만 참조 */
    private final QuizSessionId quizSessionId;

    private final UserId userId;

    /** Claude API가 생성한 개념 정리 텍스트 */
    private final String summaryContent;

    private SessionConceptSummary(SessionConceptSummaryId id, QuizSessionId quizSessionId,
                                   UserId userId, String summaryContent) {
        this.id = id;
        this.quizSessionId = quizSessionId;
        this.userId = userId;
        this.summaryContent = summaryContent;
    }

    /**
     * 세션 개념 정리를 생성한다.
     *
     * @param quizSessionId  개념 정리가 연결된 세션 ID
     * @param userId         소유자 ID
     * @param summaryContent Claude API가 생성한 개념 정리 내용
     */
    public static SessionConceptSummary create(QuizSessionId quizSessionId,
                                                UserId userId, String summaryContent) {
        return new SessionConceptSummary(
                SessionConceptSummaryId.newId(), quizSessionId, userId, summaryContent);
    }

    public SessionConceptSummaryId getId() { return id; }
    public QuizSessionId getQuizSessionId() { return quizSessionId; }
    public UserId getUserId() { return userId; }
    public String getSummaryContent() { return summaryContent; }
}
