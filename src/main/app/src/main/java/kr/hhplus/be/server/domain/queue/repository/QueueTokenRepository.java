package kr.hhplus.be.server.domain.queue.repository;

import kr.hhplus.be.server.domain.queue.QueueToken;
import java.util.List;
import java.util.Optional;

public interface QueueTokenRepository {
    QueueToken save(QueueToken token);
    Optional<QueueToken> findById(Long tokenId);
    Optional<QueueToken> findByTokenUuid(String tokenUuid);
    Optional<QueueToken> findByUserId(Long userId);
    List<QueueToken> findWaitingTokens();
    List<QueueToken> findActiveTokens();
    List<QueueToken> findExpiredTokens();
    Long countWaitingTokens();
    Long getNextQueuePosition();
}