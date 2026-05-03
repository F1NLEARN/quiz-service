package com.finlearn.quizservice.quizsession.presentation.controller;

import com.finlearn.common.response.CommonResponse;
import com.finlearn.common.util.SecurityUtil;
import com.finlearn.quizservice.quizsession.application.command.CreateLearningSessionCommand;
import com.finlearn.quizservice.quizsession.application.command.CreatePointSessionCommand;
import com.finlearn.quizservice.quizsession.application.service.CreateQuizSessionService;
import com.finlearn.quizservice.quizsession.presentation.dto.CreateLearningSessionRequest;
import com.finlearn.quizservice.quizsession.presentation.dto.CreateSessionResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 퀴즈 세션 API 컨트롤러.
 * 학습 퀴즈 세션과 포인트 퀴즈 세션의 생성 엔드포인트를 제공한다.
 */
@RestController
@RequestMapping("/api/v1/quiz-sessions")
@RequiredArgsConstructor
public class QuizSessionController {

    private final CreateQuizSessionService createQuizSessionService;

    /**
     * 학습 퀴즈 세션을 생성한다.
     * POST /api/v1/quiz-sessions/learning
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
     * POST /api/v1/quiz-sessions/point
     */
    @PostMapping("/point")
    public CommonResponse<CreateSessionResponse> createPointSession() {
        CreatePointSessionCommand command = new CreatePointSessionCommand(
                SecurityUtil.getCurrentUserId()
        );
        CreateSessionResponse response = createQuizSessionService.createPointSession(command);
        return CommonResponse.success("포인트 퀴즈 세션이 생성되었습니다.", response);
    }
}
