package com.finlearn.quizservice.quizsession.domain.service;

import com.finlearn.quizservice.quizsession.domain.vo.Score;
import com.finlearn.quizservice.quizsession.domain.vo.SeedMoney;

/**
 * 포인트 퀴즈 합격 시 시드머니 산정 도메인 서비스 인터페이스.
 * 점수 구간에 따른 지급 금액 정책이 미정이므로 인터페이스만 정의하고 구현은 TODO 처리한다.
 * Infrastructure 레이어에서 구현체를 제공한다.
 *
 * TODO: 점수 구간별 시드머니 금액 정책 확정 후 구현체 작성 필요
 */
public interface SeedMoneyPolicy {

    /**
     * 점수에 따라 지급할 시드머니를 산정한다.
     * 이 메서드는 반드시 isPassing() = true인 Score에 대해서만 호출해야 한다.
     *
     * @param score 포인트 퀴즈 최종 점수 (70점 이상)
     * @return 지급할 시드머니
     */
    SeedMoney calculate(Score score);
}
