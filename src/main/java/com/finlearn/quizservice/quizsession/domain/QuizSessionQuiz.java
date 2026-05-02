package com.finlearn.quizservice.quizsession.domain;

import com.finlearn.quizservice.quizsession.domain.vo.QuizId;
import com.finlearn.quizservice.quizsession.domain.vo.QuizSessionQuizId;

import java.time.OffsetDateTime;

/**
 * 퀴즈 세션에 포함된 개별 문제 엔티티 (QuizSession의 자식 엔티티).
 * 외부에서 직접 변경할 수 없으며, 반드시 QuizSession(Aggregate Root)을 통해서만 상태가 변경된다.
 * package-private 변경 메서드가 이 불변성을 보장한다.
 */
public class QuizSessionQuiz {

    private final QuizSessionQuizId id;
    private final QuizId quizId;
    private final int orderNo;

    /** 사용자 제출 답안 (선택지 번호). null = 미풀이 */
    private Integer submitted;

    /** 개념 정리 대상 여부. 기본값 false */
    private boolean conceptIncluded;

    /** 정오답 여부. null = 미풀이, true = 정답, false = 오답 */
    private Boolean correct;

    /** 답안 제출 시각. null = 미풀이 */
    private OffsetDateTime answeredAt;

    QuizSessionQuiz(QuizSessionQuizId id, QuizId quizId, int orderNo) {
        this.id = id;
        this.quizId = quizId;
        this.orderNo = orderNo;
        this.conceptIncluded = false;
    }

    /**
     * 답안을 제출하고 정오답을 기록한다.
     * QuizSession.submitAnswer()에서만 호출한다.
     *
     * @param submitted 사용자가 선택한 선택지 번호
     * @param correct   정답 여부
     */
    void submit(int submitted, boolean correct) {
        // 답안 기록
        this.submitted = submitted;
        // 정오답 판정 기록
        this.correct = correct;
        // 제출 시각 기록
        this.answeredAt = OffsetDateTime.now();
    }

    /**
     * 개념 정리 대상 여부를 토글한다.
     * QuizSession.toggleConceptIncluded()에서만 호출한다.
     */
    void toggleConceptIncluded() {
        this.conceptIncluded = !this.conceptIncluded;
    }

    /**
     * 개념 정리 대상으로 강제 설정한다.
     * 포인트 퀴즈 오답 시 자동으로 호출된다.
     */
    void markConceptIncluded() {
        this.conceptIncluded = true;
    }

    /** 답안이 제출된 문제인지 확인한다 */
    public boolean isAnswered() {
        return this.submitted != null;
    }

    public QuizSessionQuizId getId() { return id; }
    public QuizId getQuizId() { return quizId; }
    public int getOrderNo() { return orderNo; }
    public Integer getSubmitted() { return submitted; }
    public boolean isConceptIncluded() { return conceptIncluded; }
    public Boolean getCorrect() { return correct; }
    public OffsetDateTime getAnsweredAt() { return answeredAt; }
}
