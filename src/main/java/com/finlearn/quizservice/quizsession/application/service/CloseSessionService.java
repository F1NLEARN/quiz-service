package com.finlearn.quizservice.quizsession.application.service;

import com.finlearn.quizservice.infrastructure.repository.QuizJpaRepository;
import com.finlearn.quizservice.quizsession.application.command.CloseSessionCommand;
import com.finlearn.quizservice.quizsession.domain.QuizSession;
import com.finlearn.quizservice.quizsession.domain.exception.QuizSessionErrorCode;
import com.finlearn.quizservice.quizsession.domain.exception.QuizSessionException;
import com.finlearn.quizservice.quizsession.domain.repository.QuizSessionRepository;
import com.finlearn.quizservice.quizsession.domain.service.SeedMoneyPolicy;
import com.finlearn.quizservice.quizsession.domain.vo.QuizSessionId;
import com.finlearn.quizservice.quizsession.domain.vo.SessionType;
import com.finlearn.quizservice.quizsession.domain.vo.UserId;
import com.finlearn.quizservice.quizsession.presentation.dto.CloseSessionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 퀴즈 세션 종료 Application 서비스.
 * 세션 타입에 따라 학습/포인트 종료 로직을 분기하고 도메인 이벤트를 발행한다.
 */
@Service
@RequiredArgsConstructor
public class CloseSessionService {

    private final QuizSessionRepository quizSessionRepository;
    private final QuizJpaRepository quizJpaRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 세션을 종료하고 결과를 반환한다.
     * 학습 퀴즈는 문제별 정오답·해설을 포함하고, 포인트 퀴즈는 점수·시드머니만 반환한다.
     */
    @Transactional
    public CloseSessionResponse closeSession(CloseSessionCommand command) {
        // 세션 조회 및 소유자 검증
        QuizSession session = findSessionOrThrow(command.sessionId());
        validateOwner(session, command.userId());

        // 세션 타입별 종료 처리
        if (session.getSessionType() == SessionType.LEARNING) {
            session.closeAsLearning();
            quizSessionRepository.save(session);
            session.pullEvents().forEach(eventPublisher::publishEvent);
            // 학습 퀴즈: 문제별 정오답·해설 포함 응답
            return CloseSessionResponse.ofLearning(session, loadQuizContents(session));
        } else {
            session.closeAsPoint(new SeedMoneyPolicy());
            quizSessionRepository.save(session);
            // PointQuizPassed 이벤트 발행 (PR #5에서 Kafka 리스너 연결)
            session.pullEvents().forEach(eventPublisher::publishEvent);
            // 포인트 퀴즈: 점수·시드머니만 반환
            return CloseSessionResponse.ofPoint(session);
        }
    }

    /** 세션에 포함된 문제 ID 목록으로 Quiz 컨텐츠를 일괄 조회한다 */
    private java.util.List<com.finlearn.quizservice.domain.entity.Quiz> loadQuizContents(QuizSession session) {
        java.util.List<java.util.UUID> quizIds = session.getQuizzes().stream()
                .map(q -> q.getQuizId().value())
                .toList();
        return quizJpaRepository.findAllById(quizIds);
    }

    private QuizSession findSessionOrThrow(java.util.UUID sessionId) {
        return quizSessionRepository.findById(QuizSessionId.of(sessionId))
                .orElseThrow(() -> new QuizSessionException(QuizSessionErrorCode.SESSION_NOT_FOUND));
    }

    private void validateOwner(QuizSession session, java.util.UUID userId) {
        if (!session.getUserId().equals(UserId.of(userId))) {
            throw new QuizSessionException(QuizSessionErrorCode.SESSION_NOT_FOUND);
        }
    }
}
