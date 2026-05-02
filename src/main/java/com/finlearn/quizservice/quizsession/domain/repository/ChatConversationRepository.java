package com.finlearn.quizservice.quizsession.domain.repository;

import com.finlearn.quizservice.quizsession.domain.ChatConversation;
import com.finlearn.quizservice.quizsession.domain.vo.ChatConversationId;
import com.finlearn.quizservice.quizsession.domain.vo.QuizSessionQuizId;

import java.util.Optional;

/**
 * 챗봇 대화 도메인 Repository 인터페이스.
 * Infrastructure 레이어의 JPA 구현체가 이 인터페이스를 구현한다.
 */
public interface ChatConversationRepository {

    /** 챗봇 대화를 저장하고 저장된 객체를 반환한다 */
    ChatConversation save(ChatConversation conversation);

    /** ID로 챗봇 대화를 조회한다 */
    Optional<ChatConversation> findById(ChatConversationId id);

    /**
     * 세션-문제 ID로 챗봇 대화를 조회한다.
     * 문제별로 대화가 하나씩 생성되므로 Optional로 반환한다.
     *
     * @param quizSessionQuizId 연결된 세션-문제 ID
     */
    Optional<ChatConversation> findByQuizSessionQuizId(QuizSessionQuizId quizSessionQuizId);
}
