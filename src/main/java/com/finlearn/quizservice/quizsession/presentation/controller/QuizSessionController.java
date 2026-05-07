package com.finlearn.quizservice.quizsession.presentation.controller;

import com.finlearn.common.response.CommonResponse;
import com.finlearn.common.util.SecurityUtil;
import com.finlearn.quizservice.quizsession.application.command.CloseSessionCommand;
import com.finlearn.quizservice.quizsession.application.command.CreateLearningSessionCommand;
import com.finlearn.quizservice.quizsession.application.command.CreatePointSessionCommand;
import com.finlearn.quizservice.quizsession.application.command.GetNextQuizCommand;
import com.finlearn.quizservice.quizsession.application.command.SubmitAnswerCommand;
import com.finlearn.quizservice.quizsession.application.service.CloseSessionService;
import com.finlearn.quizservice.quizsession.application.service.CreateQuizSessionService;
import com.finlearn.quizservice.quizsession.application.service.GetNextQuizService;
import com.finlearn.quizservice.quizsession.application.service.SubmitAnswerService;
import com.finlearn.quizservice.quizsession.presentation.dto.CreateLearningSessionRequest;
import com.finlearn.quizservice.quizsession.presentation.dto.CreateSessionResponse;
import com.finlearn.quizservice.quizsession.presentation.dto.CloseSessionResponse;
import com.finlearn.quizservice.quizsession.presentation.dto.NextQuizResponse;
import com.finlearn.quizservice.quizsession.presentation.dto.SubmitAnswerRequest;
import com.finlearn.quizservice.quizsession.presentation.dto.SubmitAnswerResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * 퀴즈 세션 API 컨트롤러.
 * 세션 생성, 다음 문제 조회, 답안 제출 엔드포인트를 제공한다.
 */
@RestController
@RequestMapping("/api/v1/quiz/sessions")
@RequiredArgsConstructor
public class QuizSessionController {

    private final CreateQuizSessionService createQuizSessionService;
    private final GetNextQuizService getNextQuizService;
    private final SubmitAnswerService submitAnswerService;
    private final CloseSessionService closeSessionService;

    /**
     * 학습 퀴즈 세션을 생성한다.
     * POST /api/v1/quiz/sessions/learning
     */
    @PostMapping("/learning")
    public CommonResponse<CreateSessionResponse> createLearningSession(
            @Valid @RequestBody CreateLearningSessionRequest request) {
        // SecurityContext에서 현재 사용자 ID 추출
        CreateLearningSessionCommand command = new CreateLearningSessionCommand(
                SecurityUtil.getCurrentUserId(),
                request.category()
        );
        CreateSessionResponse response = createQuizSessionService.createLearningSession(command);
        return CommonResponse.success("학습 퀴즈 세션이 생성되었습니다.", response);
    }

    /**
     * 포인트 퀴즈 세션을 생성한다.
     * POST /api/v1/quiz/sessions/point
     */
    @PostMapping("/point")
    public CommonResponse<CreateSessionResponse> createPointSession() {
        CreatePointSessionCommand command = new CreatePointSessionCommand(
                SecurityUtil.getCurrentUserId()
        );
        CreateSessionResponse response = createQuizSessionService.createPointSession(command);
        return CommonResponse.success("포인트 퀴즈 세션이 생성되었습니다.", response);
    }

    /**
     * 다음 미풀이 문제를 조회한다.
     * GET /api/v1/quiz/sessions/{sessionId}/quizzes/next
     */
    @GetMapping("/{sessionId}/quizzes/next")
    public CommonResponse<NextQuizResponse> getNextQuiz(@PathVariable UUID sessionId) {
        GetNextQuizCommand command = new GetNextQuizCommand(sessionId, SecurityUtil.getCurrentUserId());
        NextQuizResponse response = getNextQuizService.getNextQuiz(command);
        return CommonResponse.success("다음 문제를 조회했습니다.", response);
    }

    /**
     * 답안을 제출한다.
     * POST /api/v1/quiz/sessions/{sessionId}/quizzes/{quizId}/answers
     */
    @PostMapping("/{sessionId}/quizzes/{quizId}/answers")
    public CommonResponse<SubmitAnswerResponse> submitAnswer(
            @PathVariable UUID sessionId,
            @PathVariable UUID quizId,
            @Valid @RequestBody SubmitAnswerRequest request) {
        SubmitAnswerCommand command = new SubmitAnswerCommand(
                sessionId, quizId, SecurityUtil.getCurrentUserId(), request.submitted()
        );
        SubmitAnswerResponse response = submitAnswerService.submitAnswer(command);
        return CommonResponse.success("답안이 제출되었습니다.", response);
    }

    /**
     * 퀴즈 세션을 종료한다.
     * POST /api/v1/quiz/sessions/{sessionId}/close
     */
    @PostMapping("/{sessionId}/close")
    public CommonResponse<CloseSessionResponse> closeSession(@PathVariable UUID sessionId) {
        CloseSessionCommand command = new CloseSessionCommand(sessionId, SecurityUtil.getCurrentUserId());
        CloseSessionResponse response = closeSessionService.closeSession(command);
        return CommonResponse.success("퀴즈 세션이 종료되었습니다.", response);
    }
}
