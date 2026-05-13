package com.finlearn.quizservice.quizsession.presentation.dto;

/**
 * 챗봇 AI 응답 DTO.
 *
 * @param answer AI가 생성한 응답 텍스트
 */
public record ChatMessageResponse(String answer) {}
