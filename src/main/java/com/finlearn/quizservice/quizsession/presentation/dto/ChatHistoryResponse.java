package com.finlearn.quizservice.quizsession.presentation.dto;

import java.util.List;

/**
 * 챗봇 대화 기록 조회 응답 DTO.
 *
 * @param messages 대화 메시지 목록 (생성 시각 오름차순)
 */
public record ChatHistoryResponse(List<MessageEntry> messages) {

    /**
     * 개별 메시지 항목.
     *
     * @param role    발신자 역할 ("USER" 또는 "ASSISTANT")
     * @param content 메시지 내용
     */
    public record MessageEntry(String role, String content) {}
}
