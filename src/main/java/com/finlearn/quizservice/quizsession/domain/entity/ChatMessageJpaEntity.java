package com.finlearn.quizservice.quizsession.domain.entity;

import com.finlearn.common.domain.BaseEntity;
import com.finlearn.quizservice.quizsession.domain.ChatMessage;
import com.finlearn.quizservice.quizsession.domain.vo.ChatMessageId;
import com.finlearn.quizservice.quizsession.domain.vo.MessageRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * chat_messages 테이블과 매핑되는 JPA 엔티티.
 * ChatConversationJpaEntity의 자식으로, 챗봇 대화의 개별 메시지를 나타낸다.
 */
@Entity
@Table(name = "chat_messages")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessageJpaEntity extends BaseEntity {

    @Id
    @Column(name = "chat_messages_id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_conversations_id", nullable = false)
    private ChatConversationJpaEntity conversation;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private MessageRole role;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * 도메인 객체에서 JPA 엔티티를 생성한다.
     */
    public static ChatMessageJpaEntity fromDomain(ChatMessage message,
                                                   ChatConversationJpaEntity conversationEntity) {
        ChatMessageJpaEntity entity = new ChatMessageJpaEntity();
        entity.id = message.getId().value();
        entity.conversation = conversationEntity;
        entity.role = message.getRole();
        entity.content = message.getContent();
        return entity;
    }

    /**
     * JPA 엔티티에서 도메인 객체를 재구성한다.
     */
    public ChatMessage toDomain() {
        return ChatMessage.reconstruct(ChatMessageId.of(id), role, content);
    }
}
