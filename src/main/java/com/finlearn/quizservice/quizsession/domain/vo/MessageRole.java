package com.finlearn.quizservice.quizsession.domain.vo;

/**
 * 챗봇 메시지 발신자 역할 VO.
 * USER: 사용자가 보낸 메시지.
 * ASSISTANT: Claude AI가 생성한 응답 메시지.
 */
public enum MessageRole {
    USER,
    ASSISTANT
}
