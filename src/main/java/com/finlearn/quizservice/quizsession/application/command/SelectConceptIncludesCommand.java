package com.finlearn.quizservice.quizsession.application.command;

import java.util.List;
import java.util.UUID;

/**
 * 학습 퀴즈 개념 정리 대상 문제 선택 Command.
 * 세션 종료 전에 사용자가 직접 선택한 orderNo 목록을 전달한다.
 */
public record SelectConceptIncludesCommand(
        UUID sessionId,
        UUID userId,
        List<Integer> orderNos
) {}
