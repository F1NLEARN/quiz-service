package com.finlearn.quizservice.quizsession.infrastructure.kafka.event;

import java.time.OffsetDateTime;
import java.util.UUID;


/**
 * Kafka 전송용 답안 제출 이벤트 DTO.
 * 도메인 이벤트(QuizAnswerSubmitted)를 직렬화 가능한 형태로 변환한다.
 * 수신자(학습 서비스)가 문제 풀이 여부 기록에 필요한 정보만 포함한다.
 */
public record QuizAnswerSubmittedEvent(
        UUID userId,
        OffsetDateTime answeredAt
) {
}
