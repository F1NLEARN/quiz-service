package com.finlearn.quizservice.quizsession.application.command;

import java.util.UUID;

/**
 * 다음 문제 조회 커맨드.
 *
 * @param sessionId 조회할 퀴즈 세션 ID
 * @param userId    요청자 ID (소유자 검증에 사용)
 */
public record GetNextQuizCommand(UUID sessionId, UUID userId) {
}
