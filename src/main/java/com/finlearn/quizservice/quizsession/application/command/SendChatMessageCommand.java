package com.finlearn.quizservice.quizsession.application.command;

import java.util.UUID;

/**
 * 챗봇 메시지 전송 커맨드.
 *
 * @param sessionId 퀴즈 세션 ID
 * @param orderNo   문제 순번 (챗봇은 문제별로 대화 컨텍스트를 유지한다)
 * @param userId    요청자 ID
 * @param message   사용자 입력 메시지
 */
public record SendChatMessageCommand(UUID sessionId, int orderNo, UUID userId, String message) {}
