package com.finlearn.quizservice.quizsession.domain;

import com.finlearn.quizservice.quizsession.domain.event.DomainEvent;
import com.finlearn.quizservice.quizsession.domain.event.PointQuizPassed;
import com.finlearn.quizservice.quizsession.domain.event.QuizAnswerSubmitted;
import com.finlearn.quizservice.quizsession.domain.exception.QuizSessionErrorCode;
import com.finlearn.quizservice.quizsession.domain.exception.QuizSessionException;
import com.finlearn.quizservice.quizsession.domain.service.SeedMoneyPolicy;
import com.finlearn.quizservice.quizsession.domain.vo.PassStatus;
import com.finlearn.quizservice.quizsession.domain.vo.QuizCategory;
import com.finlearn.quizservice.quizsession.domain.vo.QuizId;
import com.finlearn.quizservice.quizsession.domain.vo.SeedMoney;
import com.finlearn.quizservice.quizsession.domain.vo.SessionType;
import com.finlearn.quizservice.quizsession.domain.vo.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * QuizSession Aggregate Root 단위 테스트.
 */
class QuizSessionTest {

    private UserId userId;
    private QuizCategory category;
    private List<QuizId> quizIds;
    private SeedMoneyPolicy fixedSeedMoneyPolicy;

    @BeforeEach
    void setUp() {
        userId = UserId.of(UUID.randomUUID());
        category = QuizCategory.of("BASIC_FINANCE");
        quizIds = List.of(
                QuizId.of(UUID.randomUUID()),
                QuizId.of(UUID.randomUUID()),
                QuizId.of(UUID.randomUUID())
        );
        // 테스트용 고정 시드머니 정책
        fixedSeedMoneyPolicy = score -> SeedMoney.of(100_000L);
    }

    // ==================== 세션 생성 ====================

    @DisplayName("학습 퀴즈 세션 생성 시 IN_PROGRESS 상태로 초기화된다")
    @Test
    void createLearning_초기_상태_검증() {
        // when
        QuizSession session = QuizSession.createLearning(userId, category, quizIds);

        // then
        assertThat(session.getPassStatus()).isEqualTo(PassStatus.IN_PROGRESS);
        assertThat(session.getSessionType()).isEqualTo(SessionType.LEARNING);
        assertThat(session.getCategory()).isEqualTo(category);
        assertThat(session.getTotalCount()).isEqualTo(quizIds.size());
        assertThat(session.getCorrectCount()).isZero();
        assertThat(session.getEndedAt()).isNull();
        assertThat(session.getSeedMoney()).isNull();
    }

    @DisplayName("포인트 퀴즈 세션 생성 시 카테고리가 null이다")
    @Test
    void createPoint_카테고리_null() {
        // when
        QuizSession session = QuizSession.createPoint(userId, quizIds);

        // then
        assertThat(session.getSessionType()).isEqualTo(SessionType.POINT);
        assertThat(session.getCategory()).isNull();
    }

    // ==================== 답안 제출 ====================

    @Nested
    @DisplayName("답안 제출")
    class SubmitAnswer {

        @DisplayName("정답 제출 시 correctCount가 증가한다")
        @Test
        void submitAnswer_정답_correctCount_증가() {
            // given
            QuizSession session = QuizSession.createLearning(userId, category, quizIds);
            QuizId targetQuizId = quizIds.get(0);

            // when
            session.submitAnswer(targetQuizId, 1, true);

            // then
            assertThat(session.getCorrectCount()).isEqualTo(1);
        }

        @DisplayName("오답 제출 시 correctCount가 증가하지 않는다")
        @Test
        void submitAnswer_오답_correctCount_유지() {
            // given
            QuizSession session = QuizSession.createLearning(userId, category, quizIds);

            // when
            session.submitAnswer(quizIds.get(0), 2, false);

            // then
            assertThat(session.getCorrectCount()).isZero();
        }

        @DisplayName("답안 제출 시 QuizAnswerSubmitted 이벤트가 등록된다")
        @Test
        void submitAnswer_이벤트_등록() {
            // given
            QuizSession session = QuizSession.createLearning(userId, category, quizIds);

            // when
            session.submitAnswer(quizIds.get(0), 1, true);
            List<DomainEvent> events = session.pullEvents();

            // then
            assertThat(events).hasSize(1);
            assertThat(events.get(0)).isInstanceOf(QuizAnswerSubmitted.class);
            QuizAnswerSubmitted event = (QuizAnswerSubmitted) events.get(0);
            assertThat(event.isCorrect()).isTrue();
            assertThat(event.sessionType()).isEqualTo(SessionType.LEARNING);
        }

        @DisplayName("포인트 퀴즈 오답 제출 시 isConceptIncluded가 자동으로 true가 된다")
        @Test
        void submitAnswer_포인트퀴즈_오답_개념정리_자동설정() {
            // given
            QuizSession session = QuizSession.createPoint(userId, quizIds);
            QuizId targetQuizId = quizIds.get(0);

            // when
            session.submitAnswer(targetQuizId, 2, false);

            // then
            QuizSessionQuiz quiz = session.getQuizzes().stream()
                    .filter(q -> q.getQuizId().equals(targetQuizId))
                    .findFirst()
                    .orElseThrow();
            assertThat(quiz.isConceptIncluded()).isTrue();
        }

        @DisplayName("포인트 퀴즈 정답 제출 시 isConceptIncluded가 false로 유지된다")
        @Test
        void submitAnswer_포인트퀴즈_정답_개념정리_미설정() {
            // given
            QuizSession session = QuizSession.createPoint(userId, quizIds);
            QuizId targetQuizId = quizIds.get(0);

            // when
            session.submitAnswer(targetQuizId, 1, true);

            // then
            QuizSessionQuiz quiz = session.getQuizzes().stream()
                    .filter(q -> q.getQuizId().equals(targetQuizId))
                    .findFirst()
                    .orElseThrow();
            assertThat(quiz.isConceptIncluded()).isFalse();
        }

        @DisplayName("이미 답안을 제출한 문제에 중복 제출하면 예외가 발생한다")
        @Test
        void submitAnswer_중복제출_예외() {
            // given
            QuizSession session = QuizSession.createLearning(userId, category, quizIds);
            session.submitAnswer(quizIds.get(0), 1, true);

            // when & then
            assertThatThrownBy(() -> session.submitAnswer(quizIds.get(0), 2, false))
                    .isInstanceOf(QuizSessionException.class)
                    .satisfies(ex -> assertThat(((QuizSessionException) ex).getErrorCode())
                            .isEqualTo(QuizSessionErrorCode.ANSWER_ALREADY_SUBMITTED));
        }

        @DisplayName("세션에 포함되지 않은 문제에 답안을 제출하면 예외가 발생한다")
        @Test
        void submitAnswer_존재하지_않는_문제_예외() {
            // given
            QuizSession session = QuizSession.createLearning(userId, category, quizIds);
            QuizId unknownQuizId = QuizId.of(UUID.randomUUID());

            // when & then
            assertThatThrownBy(() -> session.submitAnswer(unknownQuizId, 1, true))
                    .isInstanceOf(QuizSessionException.class)
                    .satisfies(ex -> assertThat(((QuizSessionException) ex).getErrorCode())
                            .isEqualTo(QuizSessionErrorCode.QUIZ_NOT_IN_SESSION));
        }

        @DisplayName("종료된 세션에 답안을 제출하면 예외가 발생한다")
        @Test
        void submitAnswer_종료된_세션_예외() {
            // given
            QuizSession session = QuizSession.createLearning(userId, category, quizIds);
            session.closeAsLearning();

            // when & then
            assertThatThrownBy(() -> session.submitAnswer(quizIds.get(0), 1, true))
                    .isInstanceOf(QuizSessionException.class)
                    .satisfies(ex -> assertThat(((QuizSessionException) ex).getErrorCode())
                            .isEqualTo(QuizSessionErrorCode.SESSION_ALREADY_CLOSED));
        }
    }

    // ==================== 학습 퀴즈 종료 ====================

    @Nested
    @DisplayName("학습 퀴즈 종료")
    class CloseAsLearning {

        @DisplayName("학습 퀴즈는 점수 무관 항상 PASS로 종료된다")
        @Test
        void closeAsLearning_항상_PASS() {
            // given: 아무 문제도 풀지 않은 상태 (0점)
            QuizSession session = QuizSession.createLearning(userId, category, quizIds);

            // when
            session.closeAsLearning();

            // then
            assertThat(session.getPassStatus()).isEqualTo(PassStatus.PASS);
            assertThat(session.getEndedAt()).isNotNull();
        }

        @DisplayName("학습 퀴즈 종료 시 이벤트가 발행되지 않는다")
        @Test
        void closeAsLearning_이벤트_없음() {
            // given
            QuizSession session = QuizSession.createLearning(userId, category, quizIds);

            // when
            session.closeAsLearning();
            List<DomainEvent> events = session.pullEvents();

            // then
            assertThat(events).isEmpty();
        }

        @DisplayName("이미 종료된 학습 퀴즈 세션을 다시 종료하면 예외가 발생한다")
        @Test
        void closeAsLearning_재종료_예외() {
            // given
            QuizSession session = QuizSession.createLearning(userId, category, quizIds);
            session.closeAsLearning();

            // when & then
            assertThatThrownBy(session::closeAsLearning)
                    .isInstanceOf(QuizSessionException.class)
                    .satisfies(ex -> assertThat(((QuizSessionException) ex).getErrorCode())
                            .isEqualTo(QuizSessionErrorCode.SESSION_ALREADY_CLOSED));
        }
    }

    // ==================== 포인트 퀴즈 종료 ====================

    @Nested
    @DisplayName("포인트 퀴즈 종료")
    class CloseAsPoint {

        @DisplayName("70점 이상이면 PASS로 종료된다")
        @Test
        void closeAsPoint_70점_PASS() {
            // given: 10문제 중 7문제 정답 (70점)
            List<QuizId> ids = makeQuizIds(10);
            QuizSession session = QuizSession.createPoint(userId, ids);
            for (int i = 0; i < 7; i++) {
                session.submitAnswer(ids.get(i), 1, true);
            }
            for (int i = 7; i < 10; i++) {
                session.submitAnswer(ids.get(i), 2, false);
            }

            // when
            session.pullEvents(); // 답안 제출 이벤트 수거
            session.closeAsPoint(fixedSeedMoneyPolicy);

            // then
            assertThat(session.getPassStatus()).isEqualTo(PassStatus.PASS);
            assertThat(session.getSeedMoney()).isNotNull();
        }

        @DisplayName("69점 이하이면 FAIL로 종료된다")
        @Test
        void closeAsPoint_69점_FAIL() {
            // given: 10문제 중 6.9문제... 실제로는 6문제 정답 (6*100/10 = 60점) 아닌
            // 69/100 문제로 테스트하려면 문제 수가 100개여야 함
            // 대신 7문제 중 0정답으로 0점 테스트
            List<QuizId> ids = makeQuizIds(10);
            QuizSession session = QuizSession.createPoint(userId, ids);
            // 6문제만 정답 (60점)
            for (int i = 0; i < 6; i++) {
                session.submitAnswer(ids.get(i), 1, true);
            }
            for (int i = 6; i < 10; i++) {
                session.submitAnswer(ids.get(i), 2, false);
            }

            // when
            session.pullEvents();
            session.closeAsPoint(fixedSeedMoneyPolicy);

            // then
            assertThat(session.getPassStatus()).isEqualTo(PassStatus.FAIL);
            assertThat(session.getSeedMoney()).isNull();
        }

        @DisplayName("PASS 종료 시 PointQuizPassed 이벤트가 발행된다")
        @Test
        void closeAsPoint_PASS_이벤트_발행() {
            // given: 10문제 중 7문제 정답
            List<QuizId> ids = makeQuizIds(10);
            QuizSession session = QuizSession.createPoint(userId, ids);
            for (int i = 0; i < 7; i++) {
                session.submitAnswer(ids.get(i), 1, true);
            }
            for (int i = 7; i < 10; i++) {
                session.submitAnswer(ids.get(i), 2, false);
            }

            // when
            session.pullEvents(); // 답안 제출 이벤트 수거
            session.closeAsPoint(fixedSeedMoneyPolicy);
            List<DomainEvent> events = session.pullEvents();

            // then: PointQuizPassed 이벤트가 1개 등록되어야 함
            assertThat(events).hasSize(1);
            assertThat(events.get(0)).isInstanceOf(PointQuizPassed.class);
        }

        @DisplayName("FAIL 종료 시 PointQuizPassed 이벤트가 발행되지 않는다")
        @Test
        void closeAsPoint_FAIL_이벤트_미발행() {
            // given: 0점
            List<QuizId> ids = makeQuizIds(10);
            QuizSession session = QuizSession.createPoint(userId, ids);
            for (QuizId id : ids) {
                session.submitAnswer(id, 2, false);
            }

            // when
            session.pullEvents();
            session.closeAsPoint(fixedSeedMoneyPolicy);
            List<DomainEvent> events = session.pullEvents();

            // then
            assertThat(events).isEmpty();
        }

        @DisplayName("이미 종료된 포인트 퀴즈 세션을 다시 종료하면 예외가 발생한다")
        @Test
        void closeAsPoint_재종료_예외() {
            // given
            QuizSession session = QuizSession.createPoint(userId, quizIds);
            session.closeAsPoint(fixedSeedMoneyPolicy);

            // when & then
            assertThatThrownBy(() -> session.closeAsPoint(fixedSeedMoneyPolicy))
                    .isInstanceOf(QuizSessionException.class)
                    .satisfies(ex -> assertThat(((QuizSessionException) ex).getErrorCode())
                            .isEqualTo(QuizSessionErrorCode.SESSION_ALREADY_CLOSED));
        }
    }

    // ==================== 도메인 이벤트 ====================

    @Nested
    @DisplayName("도메인 이벤트 관리")
    class Events {

        @DisplayName("pullEvents 호출 후 이벤트 목록이 초기화된다")
        @Test
        void pullEvents_호출후_초기화() {
            // given
            QuizSession session = QuizSession.createLearning(userId, category, quizIds);
            session.submitAnswer(quizIds.get(0), 1, true);

            // when: 첫 번째 pull
            List<DomainEvent> first = session.pullEvents();
            // when: 두 번째 pull
            List<DomainEvent> second = session.pullEvents();

            // then
            assertThat(first).hasSize(1);
            assertThat(second).isEmpty();
        }

        @DisplayName("여러 답안 제출 시 제출 수만큼 이벤트가 등록된다")
        @Test
        void submitAnswer_복수_이벤트() {
            // given
            QuizSession session = QuizSession.createLearning(userId, category, quizIds);

            // when
            session.submitAnswer(quizIds.get(0), 1, true);
            session.submitAnswer(quizIds.get(1), 2, false);
            List<DomainEvent> events = session.pullEvents();

            // then
            assertThat(events).hasSize(2);
        }
    }

    // ==================== 불변성 ====================

    @DisplayName("getQuizzes가 반환하는 리스트는 수정할 수 없다")
    @Test
    void getQuizzes_불변_리스트() {
        // given
        QuizSession session = QuizSession.createLearning(userId, category, quizIds);

        // when & then
        assertThatThrownBy(() -> session.getQuizzes().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    // ==================== helpers ====================

    private List<QuizId> makeQuizIds(int count) {
        return java.util.stream.IntStream.range(0, count)
                .mapToObj(i -> QuizId.of(UUID.randomUUID()))
                .toList();
    }
}
