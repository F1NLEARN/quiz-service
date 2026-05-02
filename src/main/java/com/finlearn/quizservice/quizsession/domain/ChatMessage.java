package com.finlearn.quizservice.quizsession.domain;

import com.finlearn.quizservice.quizsession.domain.vo.ChatMessageId;
import com.finlearn.quizservice.quizsession.domain.vo.MessageRole;

/**
 * 챗봇 대화 메시지 엔티티 (ChatConversation의 자식 엔티티).
 * 외부에서 직접 생성할 수 없으며, 반드시 ChatConversation(Aggregate Root)을 통해서만 추가된다.
 */
public class ChatMessage {

    private final ChatMessageId id;
    private final MessageRole role;
    private final String content;

    ChatMessage(ChatMessageId id, MessageRole role, String content) {
        this.id = id;
        this.role = role;
        this.content = content;
    }

    /** package-private 팩토리 — ChatConversation에서만 호출 */
    static ChatMessage of(MessageRole role, String content) {
        return new ChatMessage(ChatMessageId.newId(), role, content);
    }

    public ChatMessageId getId() { return id; }
    public MessageRole getRole() { return role; }
    public String getContent() { return content; }
}
