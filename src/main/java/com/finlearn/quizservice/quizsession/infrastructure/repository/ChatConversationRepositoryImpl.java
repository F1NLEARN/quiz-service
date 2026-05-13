package com.finlearn.quizservice.quizsession.infrastructure.repository;

import com.finlearn.quizservice.quizsession.domain.ChatConversation;
import com.finlearn.quizservice.quizsession.domain.entity.ChatConversationJpaEntity;
import com.finlearn.quizservice.quizsession.domain.repository.ChatConversationRepository;
import com.finlearn.quizservice.quizsession.domain.vo.ChatConversationId;
import com.finlearn.quizservice.quizsession.domain.vo.QuizSessionQuizId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * ChatConversationRepository 도메인 인터페이스의 JPA 구현체.
 */
@Repository
@RequiredArgsConstructor
public class ChatConversationRepositoryImpl implements ChatConversationRepository {

    private final ChatConversationJpaRepository jpaRepository;

    @Override
    public ChatConversation save(ChatConversation conversation) {
        ChatConversationJpaEntity entity = ChatConversationJpaEntity.fromDomain(conversation);
        return jpaRepository.save(entity).toDomain();
    }

    @Override
    public Optional<ChatConversation> findById(ChatConversationId id) {
        return jpaRepository.findById(id.value())
                .map(ChatConversationJpaEntity::toDomain);
    }

    @Override
    public Optional<ChatConversation> findByQuizSessionQuizId(QuizSessionQuizId quizSessionQuizId) {
        return jpaRepository.findByQuizSessionQuizId(quizSessionQuizId.value())
                .map(ChatConversationJpaEntity::toDomain);
    }
}
