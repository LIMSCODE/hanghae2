package kr.hhplus.be.server.repository;

import kr.hhplus.be.server.domain.Queue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface QueueRepository extends JpaRepository<Queue, Long> {

    Optional<Queue> findByToken(String token);

    Optional<Queue> findByUserId(Long userId);

    @Query("SELECT q FROM Queue q WHERE q.status = 'WAITING' ORDER BY q.enteredAt ASC")
    List<Queue> findWaitingQueuesOrderByEnteredAt();

    @Query("SELECT COUNT(q) FROM Queue q WHERE q.status = 'WAITING' AND q.enteredAt < :enteredAt")
    Long countWaitingQueuesBefore(@Param("enteredAt") LocalDateTime enteredAt);

    @Query("SELECT q FROM Queue q WHERE q.status = 'ACTIVE' AND q.expiredAt < :now")
    List<Queue> findExpiredActiveQueues(@Param("now") LocalDateTime now);

    @Query("SELECT COUNT(q) FROM Queue q WHERE q.status = 'ACTIVE'")
    Long countActiveQueues();

    @Modifying
    @Query("UPDATE Queue q SET q.status = 'EXPIRED', q.expiredAt = :now, q.updatedAt = :now " +
           "WHERE q.status = 'ACTIVE' AND q.expiredAt < :now")
    int expireQueues(@Param("now") LocalDateTime now);

    @Query("SELECT q FROM Queue q WHERE q.status = 'WAITING' ORDER BY q.enteredAt ASC")
    List<Queue> findTopWaitingQueues(@Param("limit") int limit);
}