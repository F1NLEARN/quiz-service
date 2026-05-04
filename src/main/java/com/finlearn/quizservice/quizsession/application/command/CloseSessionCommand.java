package com.finlearn.quizservice.quizsession.application.command;

import java.util.UUID;

/**
 * 퀴즈 세션 종료 커맨드.
 *
 * @param sessionId 종료할 세션 ID
 * @param userId    요청자 ID (소유자 검증에 사용)
 */
public record CloseSessionCommand(UUID sessionId, UUID userId) {
}
