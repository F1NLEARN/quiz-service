package com.finlearn.quizservice.quizsession.infrastructure.outbox;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    /** PENDING 상태의 이벤트를 생성 시각 오름차순으로 최대 100개 조회한다 */
    List<OutboxEvent> findTop100ByStatusOrderByCreatedAtAsc(OutboxEvent.OutboxStatus status);
}
