package com.finlearn.quizservice.quizsession.application.service;

import com.finlearn.quizservice.quizsession.domain.ChatConversation;
import com.finlearn.quizservice.quizsession.domain.vo.SessionType;

/**
 * ChatbotService 내부에서 DB 조회 결과를 전달하기 위한 컨텍스트 레코드.
 *
 * loadConversation()과 AI 호출 사이에 데이터를 전달하며,
 * 트랜잭션 경계를 넘어 사용된다.
 */
record ChatContext(
        SessionType sessionType,
        String answerExplanation,
        ChatConversation conversation
) {}
