package kr.hhplus.be.server.infrastructure.queue;

import kr.hhplus.be.server.domain.queue.QueueToken;
import kr.hhplus.be.server.domain.queue.repository.QueueTokenRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JpaQueueTokenRepository extends JpaRepository<QueueToken, Long>, QueueTokenRepository {

    @Override
    default QueueToken save(QueueToken token) {
        return saveAndFlush(token);
    }

    Optional<QueueToken> findByTokenUuid(String tokenUuid);

    Optional<QueueToken> findByUserId(Long userId);

    @Query("SELECT qt FROM QueueToken qt WHERE qt.tokenStatus = 'WAITING' ORDER BY qt.queuePosition")
    List<QueueToken> findWaitingTokens();

    @Query("SELECT qt FROM QueueToken qt WHERE qt.tokenStatus = 'ACTIVE'")
    List<QueueToken> findActiveTokens();

    @Query("SELECT qt FROM QueueToken qt WHERE qt.tokenStatus = 'EXPIRED' OR (qt.expiresAt IS NOT NULL AND qt.expiresAt <= CURRENT_TIMESTAMP)")
    List<QueueToken> findExpiredTokens();

    @Query("SELECT COUNT(qt) FROM QueueToken qt WHERE qt.tokenStatus = 'WAITING'")
    Long countWaitingTokens();

    @Query("SELECT COALESCE(MAX(qt.queuePosition), 0) + 1 FROM QueueToken qt WHERE qt.tokenStatus = 'WAITING'")
    Long getNextQueuePosition();
}