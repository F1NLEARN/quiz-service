package com.finlearn.quizservice.quizsession.application.service;

import com.finlearn.quizservice.quizsession.application.command.SendChatMessageCommand;
import com.finlearn.quizservice.quizsession.domain.ChatConversation;
import com.finlearn.quizservice.quizsession.domain.QuizSession;
import com.finlearn.quizservice.quizsession.domain.QuizSessionQuiz;
import com.finlearn.quizservice.quizsession.domain.exception.QuizSessionErrorCode;
import com.finlearn.quizservice.quizsession.domain.exception.QuizSessionException;
import com.finlearn.quizservice.quizsession.domain.repository.ChatConversationRepository;
import com.finlearn.quizservice.quizsession.domain.repository.QuizSessionRepository;
import com.finlearn.quizservice.quizsession.domain.vo.QuizSessionId;
import com.finlearn.quizservice.quizsession.domain.vo.UserId;
import com.finlearn.quizservice.quizetl.application.service.QuizCacheService;
import com.finlearn.quizservice.quizetl.domain.entity.Quiz;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 챗봇 대화의 DB 조회/저장을 담당하는 트랜잭션 서비스.
 *
 * ChatbotService에서 AI 호출 전후로 트랜잭션을 분리하기 위해 별도 빈으로 추출한다.
 * 같은 클래스 내 @Transactional 메서드는 self-invocation으로 AOP가 동작하지 않기 때문이다.
 *
 * [ChatbotService.sendMessage 흐름]
 * loadConversation() - TX 1 (읽기, 즉시 종료)
 * → AI 호출            - 트랜잭션 밖 (DB 커넥션 미점유)
 * → saveConversation() - TX 2 (쓰기)
 */
@Service
@RequiredArgsConstructor
public class ChatbotConversationService {

    private final QuizSessionRepository quizSessionRepository;
    private final ChatConversationRepository chatConversationRepository;
    private final QuizCacheService quizCacheService;

    /**
     * 세션/문제/대화 기록을 조회하여 ChatContext로 반환한다.
     * 읽기 전용 트랜잭션으로 실행되며 반환 즉시 커넥션을 반납한다.
     */
    @Transactional(readOnly = true)
    public ChatContext loadConversation(SendChatMessageCommand command) {
        QuizSession session = findSessionOrThrow(command.sessionId());
        validateOwner(session, command.userId());

        QuizSessionQuiz sessionQuiz = session.findByOrderNo(command.orderNo());

        Quiz quiz = quizCacheService.findById(sessionQuiz.getQuizId().value());
        if (quiz == null) throw new QuizSessionException(QuizSessionErrorCode.QUIZ_NOT_FOUND);

        ChatConversation conversation = chatConversationRepository
                .findByQuizSessionQuizId(sessionQuiz.getId())
                .orElseGet(() -> ChatConversation.create(
                        sessionQuiz.getId(),
                        UserId.of(command.userId()),
                        session.getSessionType()));

        return new ChatContext(session.getSessionType(), quiz.getAnswerExplanation(), conversation);
    }

    /**
     * AI 응답이 추가된 대화를 저장한다.
     */
    @Transactional
    public void saveConversation(ChatConversation conversation) {
        chatConversationRepository.save(conversation);
    }

    private QuizSession findSessionOrThrow(UUID sessionId) {
        return quizSessionRepository.findById(QuizSessionId.of(sessionId))
                .orElseThrow(() -> new QuizSessionException(QuizSessionErrorCode.SESSION_NOT_FOUND));
    }

    private void validateOwner(QuizSession session, UUID userId) {
        if (!session.getUserId().equals(UserId.of(userId))) {
            throw new QuizSessionException(QuizSessionErrorCode.SESSION_NOT_FOUND);
        }
    }
}
