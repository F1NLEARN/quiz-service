package com.finlearn.quizservice.quizsession.presentation.dto;

import com.finlearn.quizservice.quizsession.domain.QuizSession;
import com.finlearn.quizservice.quizsession.domain.vo.SessionType;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 퀴즈 세션 생성 응답 DTO.
 * 생성된 세션 ID, 유형, 문제 목록을 반환하여 프론트엔드가 세션 진행을 시작할 수 있도록 한다.
 */
public record CreateSessionResponse(
        UUID sessionId,
        SessionType sessionType,
        String category,
        int totalCount,
        List<QuizItemResponse> quizzes,
        OffsetDateTime startedAt
) {

    public static CreateSessionResponse from(QuizSession session) {
        // 문제 목록을 orderNo 순서대로 변환
        List<QuizItemResponse> quizItems = session.getQuizzes().stream()
                .sorted((a, b) -> Integer.compare(a.getOrderNo(), b.getOrderNo()))
                .map(QuizItemResponse::from)
                .toList();

        return new CreateSessionResponse(
                session.getId().value(),
                session.getSessionType(),
                session.getCategory() != null ? session.getCategory().value() : null,
                session.getTotalCount(),
                quizItems,
                session.getStartedAt()
        );
    }
}
