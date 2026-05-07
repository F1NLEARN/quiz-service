package com.finlearn.quizservice.quizsession.domain.vo;

import java.util.UUID;

/**
 * 챗봇 메시지 식별자 VO.
 */
public record ChatMessageId(UUID value) {

    public static ChatMessageId of(UUID value) {
        return new ChatMessageId(value);
    }

    public static ChatMessageId newId() {
        return new ChatMessageId(UUID.randomUUID());
    }
}
