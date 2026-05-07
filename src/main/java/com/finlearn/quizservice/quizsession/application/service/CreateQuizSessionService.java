package com.finlearn.quizservice.quizsession.application.service;

import com.finlearn.quizservice.quizetl.domain.enums.MainTopic;
import com.finlearn.quizservice.quizetl.infrastructure.repository.QuizJpaRepository;
import com.finlearn.quizservice.quizsession.application.command.CreateLearningSessionCommand;
import com.finlearn.quizservice.quizsession.application.command.CreatePointSessionCommand;
import com.finlearn.quizservice.quizsession.domain.QuizSession;
import com.finlearn.quizservice.quizsession.domain.exception.QuizSessionErrorCode;
import com.finlearn.quizservice.quizsession.domain.exception.QuizSessionException;
import com.finlearn.quizservice.quizsession.domain.repository.QuizSessionRepository;
import com.finlearn.quizservice.quizsession.domain.vo.QuizCategory;
import com.finlearn.quizservice.quizsession.domain.vo.QuizId;
import com.finlearn.quizservice.quizsession.domain.vo.UserId;
import com.finlearn.quizservice.quizsession.presentation.dto.CreateSessionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

/**
 * 학습/포인트 퀴즈 세션 생성 Application 서비스.
 * 랜덤 문제 조회, 도메인 객체 생성, 저장의 흐름을 조율한다.
 */
@Service
@RequiredArgsConstructor
public class CreateQuizSessionService {

    // TODO: 출제 문제 수 정책 미정 — 추후 설정값으로 외부화 예정
    private static final int DEFAULT_QUIZ_COUNT = 10;

    private final QuizSessionRepository quizSessionRepository;
    private final QuizJpaRepository quizJpaRepository;

    /**
     * 학습 퀴즈 세션을 생성한다.
     * 선택한 카테고리(MainTopic)에서 랜덤으로 DEFAULT_QUIZ_COUNT개의 문제를 출제한다.
     *
     * @param command 사용자 ID, 카테고리 정보
     */
    @Transactional
    public CreateSessionResponse createLearningSession(CreateLearningSessionCommand command) {
        // 카테고리 문자열을 MainTopic enum으로 변환
        MainTopic mainTopic = parseMainTopic(command.category());

        // 해당 카테고리에서 랜덤 문제 ID 조회
        List<UUID> randomQuizIds = quizJpaRepository.findRandomIdsByMainTopic(
                mainTopic.name(), DEFAULT_QUIZ_COUNT);

        // QuizId VO 목록으로 변환
        List<QuizId> quizIds = randomQuizIds.stream().map(QuizId::of).toList();

        // 도메인 객체 생성
        QuizSession session = QuizSession.createLearning(
                UserId.of(command.userId()),
                QuizCategory.of(command.category()),
                quizIds
        );

        // 저장 후 응답 반환
        return CreateSessionResponse.from(quizSessionRepository.save(session));
    }

    /**
     * 포인트 퀴즈 세션을 생성한다.
     * 전 카테고리에서 랜덤으로 DEFAULT_QUIZ_COUNT개의 문제를 출제한다.
     * 해당 월에 이미 완료된 포인트 퀴즈가 있으면 예외를 던진다.
     *
     * @param command 사용자 ID
     */
    @Transactional
    public CreateSessionResponse createPointSession(CreatePointSessionCommand command) {
        UserId userId = UserId.of(command.userId());

        // 포인트 퀴즈 월 1회 제한 검증
        if (quizSessionRepository.existsCompletedPointQuizInMonth(userId, YearMonth.now())) {
            throw new QuizSessionException(QuizSessionErrorCode.POINT_QUIZ_DUPLICATE);
        }

        // 전 카테고리에서 랜덤 문제 ID 조회
        List<UUID> randomQuizIds = quizJpaRepository.findRandomIds(DEFAULT_QUIZ_COUNT);

        // QuizId VO 목록으로 변환
        List<QuizId> quizIds = randomQuizIds.stream().map(QuizId::of).toList();

        // 도메인 객체 생성
        QuizSession session = QuizSession.createPoint(userId, quizIds);

        // 저장 후 응답 반환
        return CreateSessionResponse.from(quizSessionRepository.save(session));
    }

    /** 카테고리 문자열을 MainTopic enum으로 변환한다. 유효하지 않은 값이면 예외를 던진다 */
    private MainTopic parseMainTopic(String category) {
        try {
            return MainTopic.valueOf(category);
        } catch (IllegalArgumentException e) {
            throw new QuizSessionException(QuizSessionErrorCode.INVALID_CATEGORY);
        }
    }
}
