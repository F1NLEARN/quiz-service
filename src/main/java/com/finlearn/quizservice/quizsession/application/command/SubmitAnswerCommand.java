package com.finlearn.quizservice.quizsession.application.command;

import java.util.UUID;

/**
 * 답안 제출 커맨드.
 *
 * @param sessionId 퀴즈 세션 ID
 * @param quizId    답안을 제출할 문제 ID
 * @param userId    요청자 ID (소유자 검증에 사용)
 * @param submitted 사용자가 선택한 선택지 번호
 */
public record SubmitAnswerCommand(UUID sessionId, UUID quizId, UUID userId, int submitted) {
}
