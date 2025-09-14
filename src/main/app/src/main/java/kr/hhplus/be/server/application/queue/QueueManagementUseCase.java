package kr.hhplus.be.server.application.queue;

import kr.hhplus.be.server.domain.queue.QueueToken;
import kr.hhplus.be.server.domain.queue.repository.QueueTokenRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class QueueManagementUseCase {

    private static final int MAX_ACTIVE_TOKENS = 100; // 동시 활성화 가능한 최대 토큰 수
    private static final int ACTIVE_DURATION_MINUTES = 10; // 토큰 활성 시간 (분)

    private final QueueTokenRepository queueTokenRepository;

    public QueueManagementUseCase(QueueTokenRepository queueTokenRepository) {
        this.queueTokenRepository = queueTokenRepository;
    }

    @Transactional
    public QueueTokenResult issueToken(Long userId) {
        // 이미 활성화된 토큰이 있는지 확인
        Optional<QueueToken> existingToken = queueTokenRepository.findByUserId(userId);
        if (existingToken.isPresent()) {
            QueueToken token = existingToken.get();
            if (token.isActive()) {
                return new QueueTokenResult(
                        token.getTokenUuid(),
                        token.getTokenStatus().name(),
                        token.getQueuePosition(),
                        0L, // 대기 시간 0
                        token.getExpiresAt()
                );
            }
        }

        // 새로운 토큰 발급
        Long nextPosition = queueTokenRepository.getNextQueuePosition();
        QueueToken newToken = new QueueToken(userId, nextPosition);
        queueTokenRepository.save(newToken);

        return new QueueTokenResult(
                newToken.getTokenUuid(),
                newToken.getTokenStatus().name(),
                newToken.getQueuePosition(),
                newToken.getEstimatedWaitTimeMinutes(),
                newToken.getExpiresAt()
        );
    }

    @Transactional(readOnly = true)
    public QueueStatusResult getQueueStatus(String tokenUuid) {
        QueueToken token = queueTokenRepository.findByTokenUuid(tokenUuid)
                .orElseThrow(() -> new IllegalArgumentException("Token not found"));

        return new QueueStatusResult(
                token.getTokenUuid(),
                token.getTokenStatus().name(),
                token.getQueuePosition(),
                token.getEstimatedWaitTimeMinutes(),
                token.isActive(),
                token.getExpiresAt()
        );
    }

    @Scheduled(fixedRate = 30000) // 30초마다 실행
    @Transactional
    public void processQueue() {
        // 1. 만료된 토큰들 정리
        List<QueueToken> expiredTokens = queueTokenRepository.findExpiredTokens();
        for (QueueToken token : expiredTokens) {
            if (!token.isExpired()) {
                token.expire();
                queueTokenRepository.save(token);
            }
        }

        // 2. 현재 활성화된 토큰 수 확인
        List<QueueToken> activeTokens = queueTokenRepository.findActiveTokens();
        int currentActiveCount = activeTokens.size();

        // 3. 새로운 토큰들을 활성화
        int availableSlots = MAX_ACTIVE_TOKENS - currentActiveCount;
        if (availableSlots > 0) {
            List<QueueToken> waitingTokens = queueTokenRepository.findWaitingTokens();
            int tokensToActivate = Math.min(availableSlots, waitingTokens.size());

            for (int i = 0; i < tokensToActivate; i++) {
                QueueToken token = waitingTokens.get(i);
                token.activate(ACTIVE_DURATION_MINUTES);
                queueTokenRepository.save(token);
            }

            // 4. 대기 중인 토큰들의 순서 업데이트
            updateQueuePositions();
        }
    }

    @Transactional
    public void completeToken(String tokenUuid) {
        QueueToken token = queueTokenRepository.findByTokenUuid(tokenUuid)
                .orElseThrow(() -> new IllegalArgumentException("Token not found"));

        token.complete();
        queueTokenRepository.save(token);
    }

    private void updateQueuePositions() {
        List<QueueToken> waitingTokens = queueTokenRepository.findWaitingTokens();
        for (int i = 0; i < waitingTokens.size(); i++) {
            QueueToken token = waitingTokens.get(i);
            token.updatePosition((long) (i + 1));
            queueTokenRepository.save(token);
        }
    }

    @Transactional(readOnly = true)
    public QueueStatistics getQueueStatistics() {
        Long waitingCount = queueTokenRepository.countWaitingTokens();
        List<QueueToken> activeTokens = queueTokenRepository.findActiveTokens();

        return new QueueStatistics(
                waitingCount,
                (long) activeTokens.size(),
                (long) MAX_ACTIVE_TOKENS
        );
    }

    // Result 클래스들
    public static class QueueTokenResult {
        private final String tokenUuid;
        private final String status;
        private final Long queuePosition;
        private final Long estimatedWaitTimeMinutes;
        private final LocalDateTime expiresAt;

        public QueueTokenResult(String tokenUuid, String status, Long queuePosition,
                               Long estimatedWaitTimeMinutes, LocalDateTime expiresAt) {
            this.tokenUuid = tokenUuid;
            this.status = status;
            this.queuePosition = queuePosition;
            this.estimatedWaitTimeMinutes = estimatedWaitTimeMinutes;
            this.expiresAt = expiresAt;
        }

        public String getTokenUuid() { return tokenUuid; }
        public String getStatus() { return status; }
        public Long getQueuePosition() { return queuePosition; }
        public Long getEstimatedWaitTimeMinutes() { return estimatedWaitTimeMinutes; }
        public LocalDateTime getExpiresAt() { return expiresAt; }
    }

    public static class QueueStatusResult {
        private final String tokenUuid;
        private final String status;
        private final Long queuePosition;
        private final Long estimatedWaitTimeMinutes;
        private final boolean isActive;
        private final LocalDateTime expiresAt;

        public QueueStatusResult(String tokenUuid, String status, Long queuePosition,
                                Long estimatedWaitTimeMinutes, boolean isActive, LocalDateTime expiresAt) {
            this.tokenUuid = tokenUuid;
            this.status = status;
            this.queuePosition = queuePosition;
            this.estimatedWaitTimeMinutes = estimatedWaitTimeMinutes;
            this.isActive = isActive;
            this.expiresAt = expiresAt;
        }

        public String getTokenUuid() { return tokenUuid; }
        public String getStatus() { return status; }
        public Long getQueuePosition() { return queuePosition; }
        public Long getEstimatedWaitTimeMinutes() { return estimatedWaitTimeMinutes; }
        public boolean isActive() { return isActive; }
        public LocalDateTime getExpiresAt() { return expiresAt; }
    }

    public static class QueueStatistics {
        private final Long waitingCount;
        private final Long activeCount;
        private final Long maxActiveCount;

        public QueueStatistics(Long waitingCount, Long activeCount, Long maxActiveCount) {
            this.waitingCount = waitingCount;
            this.activeCount = activeCount;
            this.maxActiveCount = maxActiveCount;
        }

        public Long getWaitingCount() { return waitingCount; }
        public Long getActiveCount() { return activeCount; }
        public Long getMaxActiveCount() { return maxActiveCount; }
    }
}