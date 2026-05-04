package com.finlearn.quizservice.quizsession.presentation.dto;

import com.finlearn.quizservice.domain.entity.Quiz;
import com.finlearn.quizservice.quizsession.domain.QuizSession;
import com.finlearn.quizservice.quizsession.domain.QuizSessionQuiz;
import com.finlearn.quizservice.quizsession.domain.vo.PassStatus;
import com.finlearn.quizservice.quizsession.domain.vo.SessionType;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 세션 종료 응답 DTO.
 * 학습 퀴즈는 quizResults(문제별 정오답·해설)를 포함하고,
 * 포인트 퀴즈는 seedMoney를 포함하며 quizResults는 null이다.
 */
public record CloseSessionResponse(
        UUID sessionId,
        SessionType sessionType,
        PassStatus passStatus,
        int score,
        int correctCount,
        int totalCount,
        Long seedMoney,
        OffsetDateTime endedAt,
        List<QuizResultResponse> quizResults
) {

    /** 학습 퀴즈 종료 응답 생성 — 문제별 결과 포함 */
    public static CloseSessionResponse ofLearning(QuizSession session, List<Quiz> quizContents) {
        // quizId → Quiz 맵으로 변환하여 O(1) 조회
        Map<UUID, Quiz> quizMap = quizContents.stream()
                .collect(Collectors.toMap(Quiz::getId, Function.identity()));

        List<QuizResultResponse> results = session.getQuizzes().stream()
                .sorted((a, b) -> Integer.compare(a.getOrderNo(), b.getOrderNo()))
                .map(sessionQuiz -> {
                    Quiz quiz = quizMap.get(sessionQuiz.getQuizId().value());
                    return QuizResultResponse.from(sessionQuiz, quiz);
                })
                .toList();

        return new CloseSessionResponse(
                session.getId().value(),
                session.getSessionType(),
                session.getPassStatus(),
                session.getScore().value(),
                session.getCorrectCount(),
                session.getTotalCount(),
                null,
                session.getEndedAt(),
                results
        );
    }

    /** 포인트 퀴즈 종료 응답 생성 — 점수·시드머니만 포함 */
    public static CloseSessionResponse ofPoint(QuizSession session) {
        Long seedMoney = session.getSeedMoney() != null ? session.getSeedMoney().value() : null;

        return new CloseSessionResponse(
                session.getId().value(),
                session.getSessionType(),
                session.getPassStatus(),
                session.getScore().value(),
                session.getCorrectCount(),
                session.getTotalCount(),
                seedMoney,
                session.getEndedAt(),
                null
        );
    }
}
