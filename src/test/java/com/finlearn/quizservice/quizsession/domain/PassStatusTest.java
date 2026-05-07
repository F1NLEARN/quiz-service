package com.finlearn.quizservice.quizsession.domain;

import com.finlearn.quizservice.quizsession.domain.vo.PassStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PassStatus VO 상태 전이 규칙 단위 테스트.
 */
class PassStatusTest {

    @DisplayName("IN_PROGRESS → PASS 전이는 허용된다")
    @Test
    void canTransitionTo_진행중_합격_허용() {
        assertThat(PassStatus.IN_PROGRESS.canTransitionTo(PassStatus.PASS)).isTrue();
    }

    @DisplayName("IN_PROGRESS → FAIL 전이는 허용된다")
    @Test
    void canTransitionTo_진행중_불합격_허용() {
        assertThat(PassStatus.IN_PROGRESS.canTransitionTo(PassStatus.FAIL)).isTrue();
    }

    @DisplayName("PASS → FAIL 전이는 불가하다")
    @Test
    void canTransitionTo_합격_불합격_불가() {
        assertThat(PassStatus.PASS.canTransitionTo(PassStatus.FAIL)).isFalse();
    }

    @DisplayName("FAIL → PASS 전이는 불가하다")
    @Test
    void canTransitionTo_불합격_합격_불가() {
        assertThat(PassStatus.FAIL.canTransitionTo(PassStatus.PASS)).isFalse();
    }

    @DisplayName("PASS → IN_PROGRESS 역방향 전이는 불가하다")
    @Test
    void canTransitionTo_합격_진행중_역방향_불가() {
        assertThat(PassStatus.PASS.canTransitionTo(PassStatus.IN_PROGRESS)).isFalse();
    }

    @DisplayName("FAIL → IN_PROGRESS 역방향 전이는 불가하다")
    @Test
    void canTransitionTo_불합격_진행중_역방향_불가() {
        assertThat(PassStatus.FAIL.canTransitionTo(PassStatus.IN_PROGRESS)).isFalse();
    }

    @DisplayName("IN_PROGRESS → IN_PROGRESS 동일 상태 전이는 불가하다")
    @Test
    void canTransitionTo_진행중_동일상태_불가() {
        assertThat(PassStatus.IN_PROGRESS.canTransitionTo(PassStatus.IN_PROGRESS)).isFalse();
    }
}
