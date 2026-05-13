package com.finlearn.quizservice.quizsession.domain.entity;

import com.finlearn.common.domain.BaseEntity;
import com.finlearn.quizservice.quizsession.domain.ChatConversation;
import com.finlearn.quizservice.quizsession.domain.ChatMessage;
import com.finlearn.quizservice.quizsession.domain.vo.ChatConversationId;
import com.finlearn.quizservice.quizsession.domain.vo.QuizSessionQuizId;
import com.finlearn.quizservice.quizsession.domain.vo.SessionType;
import com.finlearn.quizservice.quizsession.domain.vo.UserId;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * chat_conversations 테이블과 매핑되는 JPA 엔티티.
 * 도메인 객체(ChatConversation)와 분리되며, fromDomain/toDomain으로 변환한다.
 * 문제(quiz_session_quizzes)별로 1:1로 생성된다.
 */
@Entity
@Table(name = "chat_conversations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatConversationJpaEntity extends BaseEntity {

    @Id
    @Column(name = "chat_conversations_id", updatable = false, nullable = false)
    private UUID id;

    /** 연결된 세션-문제 ID (quiz_session_quizzes 테이블 FK) */
    @Column(name = "quiz_session_quizzes_id", nullable = false)
    private UUID quizSessionQuizId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "session_type", nullable = false)
    private SessionType sessionType;

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL,
               orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("createdAt ASC")
    private List<ChatMessageJpaEntity> messages = new ArrayList<>();

    /**
     * 도메인 객체에서 JPA 엔티티를 생성한다.
     * 자식 메시지 목록도 함께 변환하여 연관 관계를 설정한다.
     */
    public static ChatConversationJpaEntity fromDomain(ChatConversation conversation) {
        ChatConversationJpaEntity entity = new ChatConversationJpaEntity();
        entity.id = conversation.getId().value();
        entity.quizSessionQuizId = conversation.getQuizSessionQuizId().value();
        entity.userId = conversation.getUserId().value();
        entity.sessionType = conversation.getSessionType();
        entity.messages.clear();
        conversation.getMessages().stream()
                .map(m -> ChatMessageJpaEntity.fromDomain(m, entity))
                .forEach(entity.messages::add);
        return entity;
    }

    /**
     * JPA 엔티티에서 도메인 객체를 재구성한다.
     */
    public ChatConversation toDomain() {
        List<ChatMessage> domainMessages = messages.stream()
                .map(ChatMessageJpaEntity::toDomain)
                .toList();
        return ChatConversation.reconstruct(
                ChatConversationId.of(id),
                QuizSessionQuizId.of(quizSessionQuizId),
                UserId.of(userId),
                sessionType,
                domainMessages
        );
    }
}
