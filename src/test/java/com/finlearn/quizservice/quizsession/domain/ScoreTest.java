package com.finlearn.quizservice.quizsession.domain;

import com.finlearn.quizservice.quizsession.domain.vo.Score;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Score VO 단위 테스트.
 */
class ScoreTest {

    @DisplayName("정답 수와 전체 문제 수로 점수를 계산한다")
    @ParameterizedTest(name = "{0}/{1} = {2}점")
    @CsvSource({
            "0,  10, 0",
            "5,  10, 50",
            "7,  10, 70",
            "10, 10, 100",
            "69, 100, 69",
            "70, 100, 70",
            "71, 100, 71"
    })
    void of_계산결과_검증(int correct, int total, int expected) {
        // when
        Score score = Score.of(correct, total);

        // then
        assertThat(score.value()).isEqualTo(expected);
    }

    @DisplayName("포인트 퀴즈 70점 경계값 — 69점은 불합격")
    @Test
    void isPassing_69점_불합격() {
        // given
        Score score = Score.of(69, 100);

        // when & then
        assertThat(score.isPassing()).isFalse();
    }

    @DisplayName("포인트 퀴즈 70점 경계값 — 70점은 합격")
    @Test
    void isPassing_70점_합격() {
        // given
        Score score = Score.of(70, 100);

        // when & then
        assertThat(score.isPassing()).isTrue();
    }

    @DisplayName("포인트 퀴즈 70점 경계값 — 71점은 합격")
    @Test
    void isPassing_71점_합격() {
        // given
        Score score = Score.of(71, 100);

        // when & then
        assertThat(score.isPassing()).isTrue();
    }

    @DisplayName("전체 문제 수가 0이면 예외가 발생한다")
    @Test
    void of_totalCount_0_예외() {
        assertThatThrownBy(() -> Score.of(0, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("정답 수가 전체 문제 수보다 크면 예외가 발생한다")
    @Test
    void of_정답수_초과_예외() {
        assertThatThrownBy(() -> Score.of(11, 10))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
