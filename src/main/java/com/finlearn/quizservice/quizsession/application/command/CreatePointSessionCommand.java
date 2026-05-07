package com.finlearn.quizservice.quizsession.application.command;

import java.util.UUID;

/**
 * 포인트 퀴즈 세션 생성 커맨드.
 * Presentation 레이어에서 생성하여 Application 서비스에 전달한다.
 *
 * @param userId 세션 소유자 ID (SecurityContext에서 추출)
 */
public record CreatePointSessionCommand(UUID userId) {
}
