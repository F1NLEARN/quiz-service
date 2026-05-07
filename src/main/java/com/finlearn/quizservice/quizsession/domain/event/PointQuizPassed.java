package com.finlearn.quizservice.quizsession.domain.event;

import com.finlearn.quizservice.quizsession.domain.vo.QuizSessionId;
import com.finlearn.quizservice.quizsession.domain.vo.Score;
import com.finlearn.quizservice.quizsession.domain.vo.SeedMoney;
import com.finlearn.quizservice.quizsession.domain.vo.UserId;

import java.time.OffsetDateTime;

/**
 * 포인트 퀴즈 합격 도메인 이벤트.
 * 포인트 퀴즈 세션이 PASS(70점 이상)로 종료될 때만 발행된다.
 * FAIL 종료 시에는 발행하지 않는다.
 * 수신자: 모의투자 도메인 (시드머니 지급).
 */
public record PointQuizPassed(
        QuizSessionId sessionId,
        UserId userId,
        Score score,
        SeedMoney seedMoney,
        OffsetDateTime endedAt
) implements DomainEvent {
}
