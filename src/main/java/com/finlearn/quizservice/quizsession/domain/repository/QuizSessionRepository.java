package com.finlearn.quizservice.quizsession.domain.repository;

import com.finlearn.quizservice.quizsession.domain.QuizSession;
import com.finlearn.quizservice.quizsession.domain.vo.QuizSessionId;
import com.finlearn.quizservice.quizsession.domain.vo.SessionType;
import com.finlearn.quizservice.quizsession.domain.vo.UserId;

import java.time.YearMonth;
import java.util.Optional;

/**
 * 퀴즈 세션 도메인 Repository 인터페이스.
 * Infrastructure 레이어의 JPA 구현체가 이 인터페이스를 구현한다.
 * 도메인 계층은 프레임워크에 의존하지 않는다.
 */
public interface QuizSessionRepository {

    /** 퀴즈 세션을 저장하고 저장된 객체를 반환한다 */
    QuizSession save(QuizSession session);

    /** ID로 퀴즈 세션을 조회한다 */
    Optional<QuizSession> findById(QuizSessionId id);

    /**
     * 해당 월에 이미 완료된 포인트 퀴즈가 있는지 확인한다.
     * PASS 또는 FAIL 상태의 세션이 있으면 true를 반환한다.
     *
     * @param userId      사용자 ID
     * @param yearMonth   검사할 연월
     */
    boolean existsCompletedPointQuizInMonth(UserId userId, YearMonth yearMonth);
}
