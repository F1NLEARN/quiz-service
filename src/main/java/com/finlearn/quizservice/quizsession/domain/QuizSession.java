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
import com.finlearn.quizservice.quizsession.domain.vo.QuizSessionId;
import com.finlearn.quizservice.quizsession.domain.vo.QuizSessionQuizId;
import com.finlearn.quizservice.quizsession.domain.vo.Score;
import com.finlearn.quizservice.quizsession.domain.vo.SeedMoney;
import com.finlearn.quizservice.quizsession.domain.vo.SessionType;
import com.finlearn.quizservice.quizsession.domain.vo.UserId;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * 퀴즈 세션 Aggregate Root.
 * 학습/포인트 퀴즈 세션의 라이프사이클(생성, 진행, 종료)을 관리하고
 * 답안 제출, 채점, 도메인 이벤트 등록을 담당한다.
 * 자식 엔티티(QuizSessionQuiz)의 모든 변경은 반드시 이 Root를 통해서만 이루어진다.
 */
public class QuizSession {

    private final QuizSessionId id;
    private final UserId userId;
    private final SessionType sessionType;

    /** 학습 퀴즈 카테고리. 포인트 퀴즈는 null */
    private final QuizCategory category;

    private final int totalCount;
    private int correctCount;
    private Score score;

    /** 세션 진행 상태. 초기값 IN_PROGRESS */
    private PassStatus passStatus;

    /** 포인트 퀴즈 PASS 시 지급될 시드머니. PASS 이전에는 null */
    private SeedMoney seedMoney;

    private final OffsetDateTime startedAt;

    /** 세션 종료 시각. 종료 전에는 null */
    private OffsetDateTime endedAt;

    private final List<QuizSessionQuiz> quizzes;

    /** 미발행 도메인 이벤트 목록 */
    private final List<DomainEvent> events;

    private QuizSession(QuizSessionId id, UserId userId, SessionType sessionType,
                        QuizCategory category, List<QuizId> quizIds) {
        this.id = id;
        this.userId = userId;
        this.sessionType = sessionType;
        this.category = category;
        this.totalCount = quizIds.size();
        this.correctCount = 0;
        this.score = Score.of(0, quizIds.size());
        this.passStatus = PassStatus.IN_PROGRESS;
        this.startedAt = OffsetDateTime.now();
        this.quizzes = buildQuizList(quizIds);
        this.events = new ArrayList<>();
    }

    private QuizSession(QuizSessionId id, UserId userId, SessionType sessionType,
                        QuizCategory category, int totalCount, int correctCount, Score score,
                        PassStatus passStatus, SeedMoney seedMoney,
                        OffsetDateTime startedAt, OffsetDateTime endedAt,
                        List<QuizSessionQuiz> quizzes) {
        this.id = id;
        this.userId = userId;
        this.sessionType = sessionType;
        this.category = category;
        this.totalCount = totalCount;
        this.correctCount = correctCount;
        this.score = score;
        this.passStatus = passStatus;
        this.seedMoney = seedMoney;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.quizzes = new ArrayList<>(quizzes);
        this.events = new ArrayList<>();
    }

    /**
     * DB에서 읽어온 데이터로 도메인 객체를 재구성한다.
     * Infrastructure 레이어(Repository 구현체)에서만 호출한다.
     * createLearning/createPoint와 달리 새 ID·시각을 생성하지 않는다.
     */
    public static QuizSession reconstruct(
            QuizSessionId id, UserId userId, SessionType sessionType, QuizCategory category,
            int totalCount, int correctCount, Score score, PassStatus passStatus,
            SeedMoney seedMoney, OffsetDateTime startedAt, OffsetDateTime endedAt,
            List<QuizSessionQuiz> quizzes) {
        return new QuizSession(id, userId, sessionType, category, totalCount, correctCount,
                score, passStatus, seedMoney, startedAt, endedAt, quizzes);
    }

    /**
     * 학습 퀴즈 세션을 생성한다.
     *
     * @param userId    세션 소유자 ID
     * @param category  선택한 카테고리
     * @param quizIds   출제할 문제 ID 목록 (순서대로 order_no 부여)
     */
    public static QuizSession createLearning(UserId userId, QuizCategory category,
                                              List<QuizId> quizIds) {
        if (quizIds == null || quizIds.isEmpty()) {
            throw new IllegalArgumentException("출제할 문제 목록이 비어있습니다.");
        }
        return new QuizSession(QuizSessionId.newId(), userId, SessionType.LEARNING, category, quizIds);
    }

    /**
     * 포인트 퀴즈 세션을 생성한다.
     *
     * @param userId   세션 소유자 ID
     * @param quizIds  출제할 문제 ID 목록 (전 범위 혼합)
     */
    public static QuizSession createPoint(UserId userId, List<QuizId> quizIds) {
        if (quizIds == null || quizIds.isEmpty()) {
            throw new IllegalArgumentException("출제할 문제 목록이 비어있습니다.");
        }
        return new QuizSession(QuizSessionId.newId(), userId, SessionType.POINT, null, quizIds);
    }

    /**
     * 사용자가 제출한 답안을 처리하고 QuizAnswerSubmitted 이벤트를 등록한다.
     *
     * @param quizId    답안을 제출할 문제 ID
     * @param submitted 사용자가 선택한 선택지 번호
     * @param correct   정답 여부 (Application 레이어에서 quiz content 조회 후 전달)
     */
    public void submitAnswer(QuizId quizId, int submitted, boolean correct) {
        // 세션이 진행 중인지 검증
        validateInProgress();

        // 대상 문제 조회
        QuizSessionQuiz target = findQuiz(quizId);

        // 중복 제출 검증
        if (target.isAnswered()) {
            throw new QuizSessionException(QuizSessionErrorCode.ANSWER_ALREADY_SUBMITTED);
        }

        // 답안 제출 및 정오답 판정
        target.submit(submitted, correct);

        // 포인트 퀴즈 오답 시 자동으로 개념 정리 대상 설정
        if (sessionType == SessionType.POINT && !correct) {
            target.markConceptIncluded();
        }

        // 정답 개수 재계산
        recalculateCorrectCount();

        // 답안 제출 이벤트 등록
        events.add(new QuizAnswerSubmitted(id, userId, quizId, correct, sessionType, target.getAnsweredAt()));
    }

    /**
     * 개념 정리 대상 여부를 토글한다. 학습 퀴즈 세션에서만 사용 가능하다.
     *
     * @param quizSessionQuizId 토글할 세션-문제 ID
     */
    public void toggleConceptIncluded(QuizSessionQuizId quizSessionQuizId) {
        // 학습 퀴즈 전용 기능 검증
        if (sessionType != SessionType.LEARNING) {
            throw new QuizSessionException(QuizSessionErrorCode.LEARNING_QUIZ_ONLY);
        }

        // 세션이 진행 중인지 검증
        validateInProgress();

        // 대상 문제 조회
        QuizSessionQuiz target = findQuizById(quizSessionQuizId);

        // 개념 정리 대상 토글
        target.toggleConceptIncluded();
    }

    /**
     * 학습 퀴즈 세션을 종료한다. 점수와 무관하게 항상 PASS로 처리된다.
     */
    public void closeAsLearning() {
        // 진행 중인 세션인지 검증
        validateInProgress();

        // 최종 점수 계산
        this.score = Score.of(correctCount, totalCount);

        // 학습 퀴즈는 점수 무관 항상 PASS
        this.passStatus = PassStatus.PASS;

        // 종료 시각 기록
        this.endedAt = OffsetDateTime.now();
    }

    /**
     * 포인트 퀴즈 세션을 종료한다. 70점 이상이면 PASS, 미만이면 FAIL로 처리된다.
     * PASS 시에만 PointQuizPassed 이벤트를 등록하고 시드머니를 설정한다.
     *
     * @param seedMoneyPolicy 점수 구간별 시드머니 산정 정책
     */
    public void closeAsPoint(SeedMoneyPolicy seedMoneyPolicy) {
        // 진행 중인 세션인지 검증
        validateInProgress();

        // 최종 점수 계산
        this.score = Score.of(correctCount, totalCount);

        // 종료 시각 기록
        this.endedAt = OffsetDateTime.now();

        // 70점 이상 PASS, 미만 FAIL 판정
        if (this.score.isPassing()) {
            this.passStatus = PassStatus.PASS;
            // 시드머니 산정 및 기록
            this.seedMoney = seedMoneyPolicy.calculate(this.score);
            // PASS 시에만 이벤트 발행
            events.add(new PointQuizPassed(id, userId, score, seedMoney, endedAt));
        } else {
            this.passStatus = PassStatus.FAIL;
        }
    }

    /**
     * 등록된 도메인 이벤트를 수거하고 내부 목록을 초기화한다.
     * Application 레이어에서 트랜잭션 커밋 전에 호출해 ApplicationEventPublisher로 발행한다.
     *
     * @return 수거된 도메인 이벤트 목록
     */
    public List<DomainEvent> pullEvents() {
        // 이벤트 복사본 반환
        List<DomainEvent> copy = new ArrayList<>(events);
        // 내부 목록 초기화 (재발행 방지)
        events.clear();
        return copy;
    }

    /**
     * 아직 답안이 제출되지 않은 문제 중 orderNo가 가장 낮은 문제를 반환한다.
     * 모든 문제를 풀었으면 Optional.empty()를 반환한다.
     */
    public Optional<QuizSessionQuiz> findNextUnanswered() {
        return quizzes.stream()
                .filter(q -> !q.isAnswered())
                .min(Comparator.comparingInt(QuizSessionQuiz::getOrderNo));
    }

    /** 개념 정리 대상 문제가 1개 이상인지 확인한다 */
    public boolean hasConceptIncludedQuiz() {
        return quizzes.stream().anyMatch(QuizSessionQuiz::isConceptIncluded);
    }

    /**
     * 포함된 문제 목록을 불변 리스트로 반환한다.
     * 외부에서 직접 자식 엔티티를 수정하는 것을 방지한다.
     */
    public List<QuizSessionQuiz> getQuizzes() {
        return Collections.unmodifiableList(quizzes);
    }

    // ==================== private helpers ====================

    /** 세션이 진행 중인지 검증한다 */
    private void validateInProgress() {
        if (passStatus != PassStatus.IN_PROGRESS) {
            throw new QuizSessionException(QuizSessionErrorCode.SESSION_ALREADY_CLOSED);
        }
    }

    /** QuizId로 자식 엔티티를 조회한다 */
    private QuizSessionQuiz findQuiz(QuizId quizId) {
        return quizzes.stream()
                .filter(q -> q.getQuizId().equals(quizId))
                .findFirst()
                .orElseThrow(() -> new QuizSessionException(QuizSessionErrorCode.QUIZ_NOT_IN_SESSION));
    }

    /** QuizSessionQuizId로 자식 엔티티를 조회한다 */
    private QuizSessionQuiz findQuizById(QuizSessionQuizId quizSessionQuizId) {
        return quizzes.stream()
                .filter(q -> q.getId().equals(quizSessionQuizId))
                .findFirst()
                .orElseThrow(() -> new QuizSessionException(QuizSessionErrorCode.QUIZ_NOT_IN_SESSION));
    }

    /** 전체 문제 중 정답 수를 재계산한다 */
    private void recalculateCorrectCount() {
        this.correctCount = (int) quizzes.stream()
                .filter(q -> Boolean.TRUE.equals(q.getCorrect()))
                .count();
    }

    /** quizId 목록으로 QuizSessionQuiz 자식 엔티티 목록을 생성한다 (order_no는 1부터) */
    private List<QuizSessionQuiz> buildQuizList(List<QuizId> quizIds) {
        List<QuizSessionQuiz> list = new ArrayList<>();
        for (int i = 0; i < quizIds.size(); i++) {
            list.add(new QuizSessionQuiz(QuizSessionQuizId.newId(), quizIds.get(i), i + 1));
        }
        return list;
    }

    public QuizSessionId getId() { return id; }
    public UserId getUserId() { return userId; }
    public SessionType getSessionType() { return sessionType; }
    public QuizCategory getCategory() { return category; }
    public int getTotalCount() { return totalCount; }
    public int getCorrectCount() { return correctCount; }
    public Score getScore() { return score; }
    public PassStatus getPassStatus() { return passStatus; }
    public SeedMoney getSeedMoney() { return seedMoney; }
    public OffsetDateTime getStartedAt() { return startedAt; }
    public OffsetDateTime getEndedAt() { return endedAt; }
}
