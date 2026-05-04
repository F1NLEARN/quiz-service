package com.finlearn.quizservice.quizsession.presentation.dto;

import com.finlearn.quizservice.quizsession.domain.QuizSession;
import com.finlearn.quizservice.quizsession.domain.vo.PassStatus;
import com.finlearn.quizservice.quizsession.domain.vo.SessionType;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 세션 종료 응답 DTO.
 * 세션 타입 공통 응답으로, 포인트 퀴즈 PASS 시 seedMoney가 포함된다.
 * 해설은 챗봇 및 개념 정리 API를 통해 별도 제공된다.
 */
public record CloseSessionResponse(
        UUID sessionId,
        SessionType sessionType,
        PassStatus passStatus,
        int score,
        int correctCount,
        int totalCount,
        Long seedMoney,
        OffsetDateTime endedAt
) {

    public static CloseSessionResponse from(QuizSession session) {
        Long seedMoney = session.getSeedMoney() != null ? session.getSeedMoney().value() : null;

        return new CloseSessionResponse(
                session.getId().value(),
                session.getSessionType(),
                session.getPassStatus(),
                session.getScore().value(),
                session.getCorrectCount(),
                session.getTotalCount(),
                seedMoney,
                session.getEndedAt()
        );
    }
}
