package com.finlearn.quizservice.quizsession.application.service;

import com.finlearn.quizservice.quizsession.application.command.SendChatMessageCommand;
import com.finlearn.quizservice.quizsession.domain.ChatConversation;
import com.finlearn.quizservice.quizsession.domain.ChatMessage;
import com.finlearn.quizservice.quizsession.domain.QuizSession;
import com.finlearn.quizservice.quizsession.domain.QuizSessionQuiz;
import com.finlearn.quizservice.quizsession.domain.exception.QuizSessionErrorCode;
import com.finlearn.quizservice.quizsession.domain.exception.QuizSessionException;
import com.finlearn.quizservice.quizsession.domain.repository.ChatConversationRepository;
import com.finlearn.quizservice.quizsession.domain.repository.QuizSessionRepository;
import com.finlearn.quizservice.quizsession.domain.vo.MessageRole;
import com.finlearn.quizservice.quizsession.domain.vo.QuizSessionId;
import com.finlearn.quizservice.quizsession.domain.vo.SessionType;
import com.finlearn.quizservice.quizsession.domain.vo.UserId;
import com.finlearn.quizservice.quizsession.presentation.dto.ChatHistoryResponse;
import com.finlearn.quizservice.quizsession.presentation.dto.ChatMessageResponse;
import com.finlearn.quizservice.quizetl.domain.entity.Quiz;
import com.finlearn.quizservice.quizetl.infrastructure.repository.QuizJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 챗봇 Application 서비스.
 *
 * RAG(Retrieval-Augmented Generation) 파이프라인:
 * 1. 세션/문제 유효성 검증
 * 2. VectorStore에서 subTopic 기준으로 관련 문서 검색
 * 3. 기존 대화 기록 조회 또는 신규 생성
 * 4. 세션 유형(LEARNING/POINT)에 따른 시스템 프롬프트 분기
 * 5. ChatClient에 [시스템 프롬프트 + 히스토리 + 현재 메시지] 전달
 * 6. 사용자 메시지 + AI 응답 저장
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatbotService {

    private final QuizSessionRepository quizSessionRepository;
    private final QuizJpaRepository quizJpaRepository;
    private final ChatConversationRepository chatConversationRepository;
    private final ChatClient chatClient;

    /**
     * 사용자 메시지를 처리하고 AI 응답을 반환한다.
     */
    @Transactional
    public ChatMessageResponse sendMessage(SendChatMessageCommand command) {
        // 1. 세션 조회 및 소유자 검증
        QuizSession session = findSessionOrThrow(command.sessionId());
        validateOwner(session, command.userId());

        // 2. orderNo로 세션-문제 조회
        QuizSessionQuiz sessionQuiz = session.findByOrderNo(command.orderNo());

        // 3. Quiz 엔티티 조회 (answerExplanation 확보)
        Quiz quiz = quizJpaRepository.findById(sessionQuiz.getQuizId().value())
                .orElseThrow(() -> new QuizSessionException(QuizSessionErrorCode.QUIZ_NOT_FOUND));

        // 4. 대화 조회 또는 신규 생성 (문제별 대화 컨텍스트 유지)
        ChatConversation conversation = chatConversationRepository
                .findByQuizSessionQuizId(sessionQuiz.getId())
                .orElseGet(() -> ChatConversation.create(
                        sessionQuiz.getId(),
                        UserId.of(command.userId()),
                        session.getSessionType()));

        // 5. 세션 유형에 따른 시스템 프롬프트 생성 (answerExplanation 직접 주입)
        String systemPrompt = buildSystemPrompt(session.getSessionType(), quiz.getAnswerExplanation());

        // 6. 이전 대화 기록을 Spring AI Message 형식으로 변환
        List<Message> historyMessages = buildHistoryMessages(conversation.getMessages());

        // 7. ChatClient 호출
        String aiResponse = chatClient.prompt()
                .system(systemPrompt)
                .messages(historyMessages)
                .user(command.message())
                .call()
                .content();

        // 8. 사용자 메시지 + AI 응답을 대화에 추가하고 저장
        conversation.addUserMessage(command.message());
        conversation.addAssistantMessage(aiResponse);
        chatConversationRepository.save(conversation);

        log.info("[Chatbot] 응답 완료 - sessionId: {}, orderNo: {}", command.sessionId(), command.orderNo());

        return new ChatMessageResponse(aiResponse);
    }

    /**
     * 특정 문제의 챗봇 대화 기록을 반환한다.
     */
    @Transactional(readOnly = true)
    public ChatHistoryResponse getChatHistory(UUID sessionId, int orderNo, UUID userId) {
        QuizSession session = findSessionOrThrow(sessionId);
        validateOwner(session, userId);

        QuizSessionQuiz sessionQuiz = session.findByOrderNo(orderNo);

        List<ChatHistoryResponse.MessageEntry> messages = chatConversationRepository
                .findByQuizSessionQuizId(sessionQuiz.getId())
                .map(conv -> conv.getMessages().stream()
                        .map(m -> new ChatHistoryResponse.MessageEntry(
                                m.getRole().name(), m.getContent()))
                        .toList())
                .orElse(List.of());

        return new ChatHistoryResponse(messages);
    }

    // ==================== private helpers ====================

    private QuizSession findSessionOrThrow(UUID sessionId) {
        return quizSessionRepository.findById(QuizSessionId.of(sessionId))
                .orElseThrow(() -> new QuizSessionException(QuizSessionErrorCode.SESSION_NOT_FOUND));
    }

    /** 소유자 불일치 시 존재하지 않는 것처럼 처리 (정보 노출 방지) */
    private void validateOwner(QuizSession session, UUID userId) {
        if (!session.getUserId().equals(UserId.of(userId))) {
            throw new QuizSessionException(QuizSessionErrorCode.SESSION_NOT_FOUND);
        }
    }

    /**
     * 세션 유형에 따라 시스템 프롬프트를 생성한다.
     * - LEARNING: 정답 설명 포함 자유 응답 허용
     * - POINT: 힌트·개념 설명만, 정답 직접 언급 금지
     */
    /**
     * @param answerExplanation Quiz 엔티티의 해설 텍스트 (RAG 대신 직접 주입)
     */
    private String buildSystemPrompt(SessionType sessionType, String answerExplanation) {
        StringBuilder sb = new StringBuilder("""
            # Role: Financial Education AI Assistant
            # Constraints:
            - Use the provided [문제 해설] for accuracy.
            - Explain financial terms simply.
            - Answer in Korean (한국어로 답변하세요).
            """);

        if (sessionType == SessionType.POINT) {
            sb.append("""
                # Mode: [POINT QUIZ]
                - Provide concept hints only.
                - STRICT: Never reveal the correct answer or option.
                """);
        } else {
            sb.append("""
                # Mode: [LEARNING]
                - Provide full explanations and answers.
                """);
        }

        if (answerExplanation != null && !answerExplanation.isBlank()) {
            sb.append("\n# 문제 해설:\n").append(answerExplanation);
        }

        return sb.toString();
    }

    /** 도메인 ChatMessage 목록을 Spring AI Message 목록으로 변환한다 */
    private List<Message> buildHistoryMessages(List<ChatMessage> domainMessages) {
        List<Message> messages = new ArrayList<>();
        for (ChatMessage msg : domainMessages) {
            if (msg.getRole() == MessageRole.USER) {
                messages.add(new UserMessage(msg.getContent()));
            } else {
                messages.add(new AssistantMessage(msg.getContent()));
            }
        }
        return messages;
    }
}
