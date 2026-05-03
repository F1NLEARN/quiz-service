package com.finlearn.quizservice.quizsession.application.service;

import com.finlearn.quizservice.domain.entity.Quiz;
import com.finlearn.quizservice.infrastructure.repository.QuizJpaRepository;
import com.finlearn.quizservice.quizsession.application.command.GetNextQuizCommand;
import com.finlearn.quizservice.quizsession.domain.QuizSession;
import com.finlearn.quizservice.quizsession.domain.QuizSessionQuiz;
import com.finlearn.quizservice.quizsession.domain.exception.QuizSessionErrorCode;
import com.finlearn.quizservice.quizsession.domain.exception.QuizSessionException;
import com.finlearn.quizservice.quizsession.domain.repository.QuizSessionRepository;
import com.finlearn.quizservice.quizsession.domain.vo.QuizSessionId;
import com.finlearn.quizservice.quizsession.domain.vo.UserId;
import com.finlearn.quizservice.quizsession.presentation.dto.NextQuizResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 다음 문제 조회 Application 서비스.
 * 세션에서 아직 풀지 않은 가장 낮은 orderNo의 문제를 찾아 컨텐츠와 함께 반환한다.
 */
@Service
@RequiredArgsConstructor
public class GetNextQuizService {

    private final QuizSessionRepository quizSessionRepository;
    private final QuizJpaRepository quizJpaRepository;

    /**
     * 다음 미풀이 문제를 조회한다.
     * choices에서 isCorrect 필드를 제거하여 정답을 노출하지 않는다.
     */
    @Transactional(readOnly = true)
    public NextQuizResponse getNextQuiz(GetNextQuizCommand command) {
        // 세션 조회
        QuizSession session = findSessionOrThrow(command.sessionId());

        // 소유자 검증 — 다른 사용자의 세션은 존재하지 않는 것처럼 처리
        validateOwner(session, command.userId());

        // 다음 미풀이 문제 조회
        QuizSessionQuiz nextQuiz = session.findNextUnanswered()
                .orElseThrow(() -> new QuizSessionException(QuizSessionErrorCode.NO_REMAINING_QUIZ));

        // 문제 컨텐츠 조회
        Quiz quiz = quizJpaRepository.findById(nextQuiz.getQuizId().value())
                .orElseThrow(() -> new QuizSessionException(QuizSessionErrorCode.QUIZ_NOT_FOUND));

        return NextQuizResponse.from(nextQuiz, quiz);
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
