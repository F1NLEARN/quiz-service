package com.finlearn.quizservice.quizsession.application.service;

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
    private final ApplicationEventPublisher eventPublisher;
    private final ConceptSummaryGenerationService conceptSummaryGenerationService;

    /**
     * 세션을 종료하고 결과를 반환한다.
     */
    @Transactional
    public CloseSessionResponse closeSession(CloseSessionCommand command) {
        // 세션 조회 및 소유자 검증
        QuizSession session = findSessionOrThrow(command.sessionId());
        validateOwner(session, command.userId());

        // 세션 타입별 종료 처리
        if (session.getSessionType() == SessionType.LEARNING) {
            session.closeAsLearning();
        } else {
            session.closeAsPoint(new SeedMoneyPolicy());
        }

        quizSessionRepository.save(session);
        session.pullEvents().forEach(eventPublisher::publishEvent);

        // 개념 정리 대상 문제가 있으면 비동기로 개념 정리 생성 (세션 종료 응답에 영향 없음)
        if (session.hasConceptIncludedQuiz()) {
            conceptSummaryGenerationService.generateAsync(session);
        }

        return CloseSessionResponse.from(session);
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
