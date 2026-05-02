package com.finlearn.quizservice.quizsession.domain.event;

import com.finlearn.quizservice.quizsession.domain.vo.QuizId;
import com.finlearn.quizservice.quizsession.domain.vo.QuizSessionId;
import com.finlearn.quizservice.quizsession.domain.vo.SessionType;
import com.finlearn.quizservice.quizsession.domain.vo.UserId;

import java.time.OffsetDateTime;

/**
 * 답안 제출 도메인 이벤트.
 * 사용자가 문제에 답안을 제출할 때마다 발행된다.
 * 수신자: 학습 도메인 (잔디/데일리 로그 반영).
 */
public record QuizAnswerSubmitted(
        QuizSessionId sessionId,
        UserId userId,
        QuizId quizId,
        boolean isCorrect,
        SessionType sessionType,
        OffsetDateTime answeredAt
) implements DomainEvent {
}
