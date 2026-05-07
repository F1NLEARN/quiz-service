package com.finlearn.quizservice.quizsession.domain.vo;

/**
 * 퀴즈 세션 유형 VO.
 * LEARNING: 학습 퀴즈 — 카테고리 선택, 무제한 응시, 무조건 PASS 종료.
 * POINT: 포인트 퀴즈 — 전 범위 혼합, 월 1회 제한, 70점 기준 합격.
 */
public enum SessionType {
    LEARNING,
    POINT
}
