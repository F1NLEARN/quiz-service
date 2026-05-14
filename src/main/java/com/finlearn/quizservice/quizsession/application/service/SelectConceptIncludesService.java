package com.finlearn.quizservice.quizsession.application.service;

import com.finlearn.quizservice.quizsession.application.command.SelectConceptIncludesCommand;
import com.finlearn.quizservice.quizsession.domain.QuizSession;
import com.finlearn.quizservice.quizsession.domain.exception.QuizSessionErrorCode;
import com.finlearn.quizservice.quizsession.domain.exception.QuizSessionException;
import com.finlearn.quizservice.quizsession.domain.repository.QuizSessionRepository;
import com.finlearn.quizservice.quizsession.domain.vo.QuizSessionId;
import com.finlearn.quizservice.quizsession.domain.vo.UserId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 학습 퀴즈 개념 정리 대상 선택 Application 서비스.
 * 세션 종료 전에 사용자가 개념 정리를 원하는 문제를 일괄 지정한다.
 */
@Service
@RequiredArgsConstructor
public class SelectConceptIncludesService {

    private final QuizSessionRepository quizSessionRepository;

    @Transactional
    public void selectConceptIncludes(SelectConceptIncludesCommand command) {
        QuizSession session = findSessionOrThrow(command.sessionId());
        validateOwner(session, command.userId());

        session.selectConceptIncludes(command.orderNos());

        quizSessionRepository.save(session);
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
