package com.finlearn.quizservice.quizsession.domain.service;

import com.finlearn.quizservice.quizsession.domain.vo.Score;
import com.finlearn.quizservice.quizsession.domain.vo.SeedMoney;

/**
 * 포인트 퀴즈 합격 시 시드머니 산정 도메인 서비스.
 * 점수 구간에 따라 지급할 시드머니를 결정한다.
 *
 * 70 ~ 79점: 100만원
 * 80 ~ 89점: 150만원
 * 90 ~ 99점: 200만원
 * 100점:     400만원
 */
public class SeedMoneyPolicy {

    private static final long TIER_70 = 1_000_000L;
    private static final long TIER_80 = 1_500_000L;
    private static final long TIER_90 = 2_000_000L;
    private static final long TIER_100 = 4_000_000L;

    /**
     * 점수에 따라 지급할 시드머니를 산정한다.
     * 이 메서드는 반드시 isPassing() = true인 Score에 대해서만 호출해야 한다.
     *
     * @param score 포인트 퀴즈 최종 점수 (70점 이상)
     * @return 지급할 시드머니
     */
    public SeedMoney calculate(Score score) {
        int value = score.value();
        // 점수 구간에 따라 시드머니 결정
        if (value == 100) {
            return SeedMoney.of(TIER_100);
        } else if (value >= 90) {
            return SeedMoney.of(TIER_90);
        } else if (value >= 80) {
            return SeedMoney.of(TIER_80);
        } else {
            return SeedMoney.of(TIER_70);
        }
    }
}
