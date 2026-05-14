package com.finlearn.quizservice.quizsession.domain;

import com.finlearn.quizservice.quizsession.domain.vo.ChatConversationId;
import com.finlearn.quizservice.quizsession.domain.vo.MessageRole;
import com.finlearn.quizservice.quizsession.domain.vo.QuizSessionQuizId;
import com.finlearn.quizservice.quizsession.domain.vo.SessionType;
import com.finlearn.quizservice.quizsession.domain.vo.UserId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 챗봇 대화 Aggregate Root.
 * quiz_session_quiz_id 단위(문제별)로 생성되며 별도의 대화 컨텍스트를 유지한다.
 * 세션 유형(LEARNING/POINT)에 따라 Application 레이어에서 시스템 프롬프트를 분기한다.
 * 자식 엔티티(ChatMessage)의 추가는 반드시 이 Root를 통해서만 이루어진다.
 */
public class ChatConversation {

    private final ChatConversationId id;

    /** Aggregate 간 직접 객체 참조 금지 — ID로만 참조 */
    private final QuizSessionQuizId quizSessionQuizId;

    private final UserId userId;

    /** LEARNING: 정답 포함 자유 응답 / POINT: 힌트·개념 설명만, 정답 차단 */
    private final SessionType sessionType;

    private final List<ChatMessage> messages;

    private ChatConversation(ChatConversationId id, QuizSessionQuizId quizSessionQuizId,
                              UserId userId, SessionType sessionType) {
        this.id = id;
        this.quizSessionQuizId = quizSessionQuizId;
        this.userId = userId;
        this.sessionType = sessionType;
        this.messages = new ArrayList<>();
    }

    /**
     * 문제별 챗봇 대화를 생성한다.
     *
     * @param quizSessionQuizId 대화가 연결된 세션-문제 ID
     * @param userId            대화 소유자 ID
     * @param sessionType       세션 유형 (시스템 프롬프트 분기용)
     */
    public static ChatConversation create(QuizSessionQuizId quizSessionQuizId,
                                          UserId userId, SessionType sessionType) {
        return new ChatConversation(ChatConversationId.newId(), quizSessionQuizId, userId, sessionType);
    }

    /**
     * DB에서 읽어온 데이터로 도메인 객체를 재구성한다.
     * Infrastructure 레이어(Repository 구현체)에서만 호출한다.
     */
    public static ChatConversation reconstruct(ChatConversationId id,
                                               QuizSessionQuizId quizSessionQuizId,
                                               UserId userId, SessionType sessionType,
                                               List<ChatMessage> existingMessages) {
        ChatConversation conversation = new ChatConversation(id, quizSessionQuizId, userId, sessionType);
        conversation.messages.addAll(existingMessages);
        return conversation;
    }

    /**
     * 사용자 메시지를 대화에 추가한다.
     *
     * @param content 사용자가 입력한 텍스트
     */
    public void addUserMessage(String content) {
        // 사용자 메시지 생성 및 추가
        messages.add(ChatMessage.of(MessageRole.USER, content));
    }

    /**
     * Claude AI의 응답 메시지를 대화에 추가한다.
     *
     * @param content Claude API가 반환한 응답 텍스트
     */
    public void addAssistantMessage(String content) {
        // AI 응답 메시지 생성 및 추가
        messages.add(ChatMessage.of(MessageRole.ASSISTANT, content));
    }

    /**
     * 메시지 목록을 불변 리스트로 반환한다.
     * 외부에서 직접 메시지를 수정하는 것을 방지한다.
     */
    public List<ChatMessage> getMessages() {
        return Collections.unmodifiableList(messages);
    }

    public ChatConversationId getId() { return id; }
    public QuizSessionQuizId getQuizSessionQuizId() { return quizSessionQuizId; }
    public UserId getUserId() { return userId; }
    public SessionType getSessionType() { return sessionType; }
}
