package kr.hhplus.be.server.domain.ecommerce.repository;

import kr.hhplus.be.server.domain.ecommerce.Outbox;
import java.util.List;
import java.util.Optional;

public interface OutboxRepository {
    Outbox save(Outbox outbox);
    Optional<Outbox> findById(Long outboxId);
    List<Outbox> findPendingEvents();
    List<Outbox> findFailedEventsForRetry();
    void deleteProcessedEvents();
}