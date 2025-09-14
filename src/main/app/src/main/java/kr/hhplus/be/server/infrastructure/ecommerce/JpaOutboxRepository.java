package kr.hhplus.be.server.infrastructure.ecommerce;

import kr.hhplus.be.server.domain.ecommerce.Outbox;
import kr.hhplus.be.server.domain.ecommerce.repository.OutboxRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

interface JpaOutboxRepositoryInterface extends JpaRepository<Outbox, Long> {
    @Query("SELECT o FROM Outbox o WHERE o.status = 'PENDING' ORDER BY o.createdAt ASC")
    List<Outbox> findPendingEvents();

    @Query("SELECT o FROM Outbox o WHERE o.status = 'FAILED' AND o.retryCount < 3 ORDER BY o.createdAt ASC")
    List<Outbox> findFailedEventsForRetry();

    @Modifying
    @Query("DELETE FROM Outbox o WHERE o.status = 'PROCESSED' AND o.processedAt < CURRENT_TIMESTAMP - INTERVAL 7 DAY")
    void deleteProcessedEvents();
}

@Repository
public class JpaOutboxRepository implements OutboxRepository {

    private final JpaOutboxRepositoryInterface jpaRepository;

    public JpaOutboxRepository(JpaOutboxRepositoryInterface jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Outbox save(Outbox outbox) {
        return jpaRepository.save(outbox);
    }

    @Override
    public Optional<Outbox> findById(Long outboxId) {
        return jpaRepository.findById(outboxId);
    }

    @Override
    public List<Outbox> findPendingEvents() {
        return jpaRepository.findPendingEvents();
    }

    @Override
    public List<Outbox> findFailedEventsForRetry() {
        return jpaRepository.findFailedEventsForRetry();
    }

    @Override
    @Transactional
    public void deleteProcessedEvents() {
        jpaRepository.deleteProcessedEvents();
    }
}