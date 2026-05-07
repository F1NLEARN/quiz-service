package com.finlearn.quizservice.quizsession.domain.vo;

/**
 * 퀴즈 세션 진행 상태 VO.
 * 허용되는 상태 전이는 IN_PROGRESS → PASS 또는 IN_PROGRESS → FAIL 뿐이다.
 * 역방향 전이와 PASS ↔ FAIL 전이는 불가하다.
 */
public enum PassStatus {

    IN_PROGRESS,
    PASS,
    FAIL;

    /**
     * 현재 상태에서 목표 상태로 전이가 가능한지 검증한다.
     *
     * @param next 전이하려는 목표 상태
     * @return 전이 가능 여부
     */
    public boolean canTransitionTo(PassStatus next) {
        // IN_PROGRESS → PASS 또는 FAIL만 허용
        return this == IN_PROGRESS && (next == PASS || next == FAIL);
    }
}
