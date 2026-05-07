package com.finlearn.quizservice.quizsession.domain.vo;

/**
 * 시드머니 VO.
 * 포인트 퀴즈 PASS 시 모의투자 도메인에 지급될 금액을 보관한다.
 */
public record SeedMoney(long value) {

    public static SeedMoney of(long value) {
        if (value < 0) {
            throw new IllegalArgumentException("시드머니는 0 이상이어야 합니다.");
        }
        return new SeedMoney(value);
    }
}
