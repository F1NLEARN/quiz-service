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
 * AI 호출 구간에서 DB 커넥션을 점유하지 않도록 트랜잭션을 분리한다.
 *
 * sendMessage 흐름:
 * 1. loadConversation()  — TX 1 (readOnly, 즉시 종료) → DB 커넥션 반납
 * 2. AI 호출             — 트랜잭션 밖 (커넥션 풀 미점유, ~20초 소요 가능)
 * 3. saveConversation()  — TX 2 (write)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatbotService {

    private final ChatbotConversationService chatbotConversationService;
    private final ChatConversationRepository chatConversationRepository;
    private final QuizSessionRepository quizSessionRepository;
    private final ChatClient chatClient;

    /**
     * 사용자 메시지를 처리하고 AI 응답을 반환한다.
     * @Transactional 없음 — 트랜잭션은 load/save 각각에서 독립적으로 관리된다.
     */
    public ChatMessageResponse sendMessage(SendChatMessageCommand command) {
        // 1. DB 조회 (TX 1 — readOnly, 메서드 반환 즉시 커넥션 반납)
        ChatContext ctx = chatbotConversationService.loadConversation(command);

        // 2. 시스템 프롬프트 및 히스토리 구성
        String systemPrompt = buildSystemPrompt(ctx.sessionType(), ctx.answerExplanation());
        List<Message> historyMessages = buildHistoryMessages(ctx.conversation().getMessages());

        // 3. AI 호출 (트랜잭션 밖 — DB 커넥션 미점유)
        String aiResponse = chatClient.prompt()
                .system(systemPrompt)
                .messages(historyMessages)
                .user(command.message())
                .call()
                .content();

        // 4. 대화에 메시지 추가 후 저장 (TX 2)
        ctx.conversation().addUserMessage(command.message());
        ctx.conversation().addAssistantMessage(aiResponse);
        chatbotConversationService.saveConversation(ctx.conversation());

        log.info("[Chatbot] 응답 완료 - sessionId: {}, orderNo: {}", command.sessionId(), command.orderNo());

        return new ChatMessageResponse(aiResponse);
    }

    /**
     * 특정 문제의 챗봇 대화 기록을 반환한다.
     */
    @Transactional(readOnly = true)
    public ChatHistoryResponse getChatHistory(UUID sessionId, int orderNo, UUID userId) {
        QuizSession session = quizSessionRepository.findById(QuizSessionId.of(sessionId))
                .orElseThrow(() -> new QuizSessionException(QuizSessionErrorCode.SESSION_NOT_FOUND));

        if (!session.getUserId().equals(UserId.of(userId))) {
            throw new QuizSessionException(QuizSessionErrorCode.SESSION_NOT_FOUND);
        }

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

    /**
     * 세션 유형에 따라 시스템 프롬프트를 생성한다.
     * - LEARNING: 정답 설명 포함 자유 응답 허용
     * - POINT: 힌트·개념 설명만, 정답 직접 언급 금지
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
