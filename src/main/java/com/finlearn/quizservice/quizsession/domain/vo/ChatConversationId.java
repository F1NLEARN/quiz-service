package com.finlearn.quizservice.quizsession.domain.vo;

import java.util.UUID;

/**
 * 챗봇 대화 식별자 VO.
 */
public record ChatConversationId(UUID value) {

    public static ChatConversationId of(UUID value) {
        return new ChatConversationId(value);
    }

    public static ChatConversationId newId() {
        return new ChatConversationId(UUID.randomUUID());
    }
}
