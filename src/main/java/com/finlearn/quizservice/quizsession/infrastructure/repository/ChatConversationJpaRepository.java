package com.finlearn.quizservice.quizsession.infrastructure.repository;

import com.finlearn.quizservice.quizsession.domain.entity.ChatConversationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ChatConversationJpaRepository extends JpaRepository<ChatConversationJpaEntity, UUID> {

    /** 세션-문제 ID로 챗봇 대화를 조회한다 */
    Optional<ChatConversationJpaEntity> findByQuizSessionQuizId(UUID quizSessionQuizId);
}
