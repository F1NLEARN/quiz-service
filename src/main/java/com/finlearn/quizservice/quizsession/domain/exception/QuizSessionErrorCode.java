package com.finlearn.quizservice.quizsession.domain.exception;

import org.springframework.http.HttpStatus;

/**
 * 퀴즈 세션 도메인 에러 코드.
 * 각 코드는 HTTP 상태, 고유 코드 문자열, 사용자 메시지를 가진다.
 */
public enum QuizSessionErrorCode {

    SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "QUIZ_SESSION_001", "퀴즈 세션을 찾을 수 없습니다."),
    SESSION_ALREADY_CLOSED(HttpStatus.CONFLICT, "QUIZ_SESSION_002", "이미 종료된 세션입니다."),
    QUIZ_NOT_IN_SESSION(HttpStatus.BAD_REQUEST, "QUIZ_SESSION_003", "해당 세션에 포함된 문제가 아닙니다."),
    ANSWER_ALREADY_SUBMITTED(HttpStatus.CONFLICT, "QUIZ_SESSION_004", "이미 답안을 제출한 문제입니다."),
    POINT_QUIZ_DUPLICATE(HttpStatus.CONFLICT, "QUIZ_SESSION_005", "이번 달에 이미 포인트 퀴즈를 응시했습니다."),
    CONVERSATION_NOT_FOUND(HttpStatus.NOT_FOUND, "QUIZ_SESSION_006", "챗봇 대화를 찾을 수 없습니다."),
    INVALID_SESSION_STATE(HttpStatus.BAD_REQUEST, "QUIZ_SESSION_007", "유효하지 않은 세션 상태 전이입니다."),
    CONCEPT_SUMMARY_NOT_FOUND(HttpStatus.NOT_FOUND, "QUIZ_SESSION_008", "개념 정리를 찾을 수 없습니다."),
    LEARNING_QUIZ_ONLY(HttpStatus.BAD_REQUEST, "QUIZ_SESSION_009", "학습 퀴즈 세션에서만 사용할 수 있는 기능입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    QuizSessionErrorCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }

    public HttpStatus getHttpStatus() { return httpStatus; }
    public String getCode() { return code; }
    public String getMessage() { return message; }
}
