package com.finlearn.quizservice.quizsession.infrastructure.kafka.event;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Kafka 전송용 포인트 퀴즈 PASS 이벤트 DTO.
 * 도메인 이벤트(PointQuizPassed)를 직렬화 가능한 형태로 변환한다.
 * 수신자(모의투자 서비스)가 시드머니 지급에 필요한 정보만 포함한다.
 */
public record PointQuizPassedEvent(
        UUID userId,
        long seedMoney,
        OffsetDateTime endedAt
) {
}
