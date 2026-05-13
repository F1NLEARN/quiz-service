package com.finlearn.quizservice.quizsession.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 챗봇 메시지 전송 요청 DTO.
 *
 * @param message 사용자 입력 메시지 (공백 불가, 최대 1000자)
 */
public record ChatMessageRequest(
        @NotBlank(message = "메시지를 입력해주세요.")
        @Size(max = 1000, message = "메시지는 1000자 이하여야 합니다.")
        String message
) {}
