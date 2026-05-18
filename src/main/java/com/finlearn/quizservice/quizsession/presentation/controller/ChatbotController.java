package com.finlearn.quizservice.quizsession.presentation.controller;

import com.finlearn.common.response.CommonResponse;
import com.finlearn.common.util.SecurityUtil;
import com.finlearn.quizservice.quizsession.application.command.SendChatMessageCommand;
import com.finlearn.quizservice.quizsession.application.service.ChatbotService;
import com.finlearn.quizservice.quizsession.presentation.dto.ChatHistoryResponse;
import com.finlearn.quizservice.quizsession.presentation.dto.ChatMessageRequest;
import com.finlearn.quizservice.quizsession.presentation.dto.ChatMessageResponse;
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
 * 챗봇 API 컨트롤러.
 * 문제별로 독립된 대화 컨텍스트를 유지하며, 세션 유형에 따라 응답이 달라진다.
 *
 * - LEARNING 세션: 정답 포함 자유 응답
 * - POINT 세션: 힌트·개념 설명만, 정답 차단
 */
@RestController
@RequestMapping("/api/v1/quiz/sessions")
@RequiredArgsConstructor
public class ChatbotController {

    private final ChatbotService chatbotService;

    /**
     * 챗봇에 메시지를 전송하고 AI 응답을 받는다.
     * POST /api/quiz/sessions/{sessionId}/quizzes/{orderNo}/chat
     */
    @PostMapping("/{sessionId}/quizzes/{orderNo}/chat")
    public CommonResponse<ChatMessageResponse> sendMessage(
            @PathVariable UUID sessionId,
            @PathVariable int orderNo,
            @Valid @RequestBody ChatMessageRequest request) {
        SendChatMessageCommand command = new SendChatMessageCommand(
                sessionId, orderNo, SecurityUtil.getCurrentUserId(), request.message());
        ChatMessageResponse response = chatbotService.sendMessage(command);
        return CommonResponse.success("챗봇 응답을 받았습니다.", response);
    }

    /**
     * 특정 문제의 챗봇 대화 기록을 조회한다.
     * GET /api/quiz/sessions/{sessionId}/quizzes/{orderNo}/chat
     */
    @GetMapping("/{sessionId}/quizzes/{orderNo}/chat")
    public CommonResponse<ChatHistoryResponse> getChatHistory(
            @PathVariable UUID sessionId,
            @PathVariable int orderNo) {
        ChatHistoryResponse response = chatbotService.getChatHistory(
                sessionId, orderNo, SecurityUtil.getCurrentUserId());
        return CommonResponse.success("대화 기록을 조회했습니다.", response);
    }
}
