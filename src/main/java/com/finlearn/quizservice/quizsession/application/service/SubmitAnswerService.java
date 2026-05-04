package com.finlearn.quizservice.quizsession.application.service;

import com.finlearn.quizservice.domain.entity.Quiz;
import com.finlearn.quizservice.domain.entity.QuizChoice;
import com.finlearn.quizservice.infrastructure.repository.QuizJpaRepository;
import com.finlearn.quizservice.quizsession.application.command.SubmitAnswerCommand;
import com.finlearn.quizservice.quizsession.domain.QuizSession;
import com.finlearn.quizservice.quizsession.domain.exception.QuizSessionErrorCode;
import com.finlearn.quizservice.quizsession.domain.exception.QuizSessionException;
import com.finlearn.quizservice.quizsession.domain.repository.QuizSessionRepository;
import com.finlearn.quizservice.quizsession.domain.vo.QuizId;
import com.finlearn.quizservice.quizsession.domain.vo.QuizSessionId;
import com.finlearn.quizservice.quizsession.domain.vo.UserId;
import com.finlearn.quizservice.quizsession.presentation.dto.SubmitAnswerResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 답안 제출 Application 서비스.
 * 제출된 선택지 번호와 정답을 비교하여 정오답을 판정하고 도메인 이벤트를 발행한다.
 * 해설은 챗봇을 통해 제공되므로 응답에 포함하지 않는다.
 */
@Service
@RequiredArgsConstructor
public class SubmitAnswerService {

    private final QuizSessionRepository quizSessionRepository;
    private final QuizJpaRepository quizJpaRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 답안을 제출하고 정오답 결과를 반환한다.
     */
    @Transactional
    public SubmitAnswerResponse submitAnswer(SubmitAnswerCommand command) {
        // 세션 조회 및 소유자 검증
        QuizSession session = findSessionOrThrow(command.sessionId());
        validateOwner(session, command.userId());

        // 문제 컨텐츠 조회
        Quiz quiz = quizJpaRepository.findById(command.quizId())
                .orElseThrow(() -> new QuizSessionException(QuizSessionErrorCode.QUIZ_NOT_FOUND));

        // choices에서 정답 선택지 번호 추출
        int correctNo = extractCorrectNo(quiz);

        // 제출 답안과 정답 비교
        boolean correct = command.submitted() == correctNo;

        // 도메인에 답안 제출 위임
        session.submitAnswer(QuizId.of(command.quizId()), command.submitted(), correct);

        // 저장
        quizSessionRepository.save(session);

        // 도메인 이벤트 발행 (Kafka 연동은 PR #5에서 처리)
        session.pullEvents().forEach(eventPublisher::publishEvent);

        return SubmitAnswerResponse.of(command.quizId(), command.submitted(), correct, correctNo);
    }

    /** choices 목록에서 isCorrect가 true인 선택지의 no를 추출한다 */
    private int extractCorrectNo(Quiz quiz) {
        return quiz.getChoices().stream()
                .filter(QuizChoice::isCorrect)
                .findFirst()
                .map(QuizChoice::getNo)
                .orElseThrow(() -> new QuizSessionException(QuizSessionErrorCode.QUIZ_NOT_FOUND));
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
