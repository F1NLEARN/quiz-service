package com.finlearn.quizservice.quizsession.domain.vo;

/**
 * 퀴즈 세션 점수 VO.
 * 정답 수 / 전체 문제 수 * 100 으로 계산된 정수 점수(0~100)를 보관한다.
 * 포인트 퀴즈 합격 기준(70점) 판정 책임을 가진다.
 */
public record Score(int value) {

    /** 포인트 퀴즈 합격 기준 점수 */
    private static final int PASS_THRESHOLD = 70;

    /**
     * 정답 수와 전체 문제 수로 점수를 계산한다.
     *
     * @param correctCount 정답 수 (0 이상)
     * @param totalCount   전체 문제 수 (1 이상)
     */
    public static Score of(int correctCount, int totalCount) {
        if (totalCount <= 0) {
            throw new IllegalArgumentException("전체 문제 수는 0보다 커야 합니다.");
        }
        if (correctCount < 0 || correctCount > totalCount) {
            throw new IllegalArgumentException("정답 수는 0 이상 전체 문제 수 이하여야 합니다.");
        }
        // 정수 나눗셈으로 소수점 이하 버림 (예: 7/10*100 = 70)
        return new Score(correctCount * 100 / totalCount);
    }

    /** 포인트 퀴즈 합격 여부 (70점 이상) */
    public boolean isPassing() {
        return value >= PASS_THRESHOLD;
    }
}
