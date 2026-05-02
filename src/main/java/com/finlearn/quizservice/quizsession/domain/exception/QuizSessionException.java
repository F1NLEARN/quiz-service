package com.finlearn.quizservice.quizsession.domain.exception;

import com.finlearn.common.exception.CustomException;

/**
 * 퀴즈 세션 도메인 예외.
 * QuizSessionErrorCode를 래핑해 도메인 규칙 위반 시 던진다.
 */
public class QuizSessionException extends CustomException {

    private final QuizSessionErrorCode errorCode;

    public QuizSessionException(QuizSessionErrorCode errorCode) {
        super(errorCode.getCode(), errorCode.getMessage(), errorCode.getHttpStatus());
        this.errorCode = errorCode;
    }

    public QuizSessionErrorCode getErrorCode() {
        return errorCode;
    }
}
