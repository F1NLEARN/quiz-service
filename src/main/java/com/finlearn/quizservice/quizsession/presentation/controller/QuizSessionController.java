package com.finlearn.quizservice.quizsession.presentation.controller;

import com.finlearn.common.response.CommonResponse;
import com.finlearn.common.util.SecurityUtil;
import com.finlearn.quizservice.quizsession.application.command.CloseSessionCommand;
import com.finlearn.quizservice.quizsession.application.command.CreateLearningSessionCommand;
import com.finlearn.quizservice.quizsession.application.command.CreatePointSessionCommand;
import com.finlearn.quizservice.quizsession.application.command.GetQuizCommand;
import com.finlearn.quizservice.quizsession.application.command.SubmitAnswerCommand;
import com.finlearn.quizservice.quizsession.application.service.CloseSessionService;
import com.finlearn.quizservice.quizsession.application.service.CreateQuizSessionService;
import com.finlearn.quizservice.quizsession.application.service.GetQuizService;
import com.finlearn.quizservice.quizsession.application.service.SubmitAnswerService;
import com.finlearn.quizservice.quizsession.presentation.dto.CreateLearningSessionRequest;
import com.finlearn.quizservice.quizsession.presentation.dto.CreateSessionResponse;
import com.finlearn.quizservice.quizsession.presentation.dto.CloseSessionResponse;
import com.finlearn.quizservice.quizsession.presentation.dto.QuizResponse;
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
@RequestMapping("/api/quiz/sessions")
@RequiredArgsConstructor
public class QuizSessionController {

    private final CreateQuizSessionService createQuizSessionService;
    private final GetQuizService getQuizService;
    private final SubmitAnswerService submitAnswerService;
    private final CloseSessionService closeSessionService;

    /**
     * 학습 퀴즈 세션을 생성한다.
     * POST /api/quiz/sessions/learning
     */
    @PostMapping("/learning")
    public CommonResponse<CreateSessionResponse> createLearningSession(
            @Valid @RequestBody CreateLearningSessionRequest request) {
        CreateLearningSessionCommand command = new CreateLearningSessionCommand(
                SecurityUtil.getCurrentUserId(),
                request.category()
        );
        CreateSessionResponse response = createQuizSessionService.createLearningSession(command);
        return CommonResponse.success("학습 퀴즈 세션이 생성되었습니다.", response);
    }

    /**
     * 포인트 퀴즈 세션을 생성한다.
     * POST /api/quiz/sessions/point
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
     * orderNo에 해당하는 문제를 조회한다.
     * GET /api/quiz/sessions/{sessionId}/quizzes/{orderNo}
     */
    @GetMapping("/{sessionId}/quizzes/{orderNo}")
    public CommonResponse<QuizResponse> getQuiz(@PathVariable UUID sessionId,
                                                 @PathVariable int orderNo) {
        GetQuizCommand command = new GetQuizCommand(sessionId, SecurityUtil.getCurrentUserId(), orderNo);
        QuizResponse response = getQuizService.getQuiz(command);
        return CommonResponse.success("문제를 조회했습니다.", response);
    }

    /**
     * 답안을 제출한다.
     * POST /api/quiz/sessions/{sessionId}/quizzes/{orderNo}/answers
     */
    @PostMapping("/{sessionId}/quizzes/{orderNo}/answers")
    public CommonResponse<SubmitAnswerResponse> submitAnswer(
            @PathVariable UUID sessionId,
            @PathVariable int orderNo,
            @Valid @RequestBody SubmitAnswerRequest request) {
        SubmitAnswerCommand command = new SubmitAnswerCommand(
                sessionId, orderNo, SecurityUtil.getCurrentUserId(), request.submitted()
        );
        SubmitAnswerResponse response = submitAnswerService.submitAnswer(command);
        return CommonResponse.success("답안이 제출되었습니다.", response);
    }

    /**
     * 퀴즈 세션을 종료한다.
     * POST /api/quiz/sessions/{sessionId}/close
     */
    @PostMapping("/{sessionId}/close")
    public CommonResponse<CloseSessionResponse> closeSession(@PathVariable UUID sessionId) {
        CloseSessionCommand command = new CloseSessionCommand(sessionId, SecurityUtil.getCurrentUserId());
        CloseSessionResponse response = closeSessionService.closeSession(command);
        return CommonResponse.success("퀴즈 세션이 종료되었습니다.", response);
    }
}
