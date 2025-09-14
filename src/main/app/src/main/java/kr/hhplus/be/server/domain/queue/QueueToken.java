package kr.hhplus.be.server.domain.queue;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "queue_tokens", indexes = {
    @Index(name = "idx_queue_token_uuid", columnList = "token_uuid"),
    @Index(name = "idx_queue_token_status", columnList = "token_status"),
    @Index(name = "idx_queue_token_position", columnList = "queue_position")
})
public class QueueToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "token_id")
    private Long tokenId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "token_uuid", nullable = false, unique = true, length = 36)
    private String tokenUuid;

    @Enumerated(EnumType.STRING)
    @Column(name = "token_status", nullable = false, length = 20)
    private TokenStatus tokenStatus = TokenStatus.WAITING;

    @Column(name = "queue_position", nullable = false)
    private Long queuePosition;

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    @Column(name = "activated_at")
    private LocalDateTime activatedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected QueueToken() {
    }

    public QueueToken(Long userId, Long queuePosition) {
        this.userId = userId;
        this.tokenUuid = UUID.randomUUID().toString();
        this.queuePosition = queuePosition;
        this.tokenStatus = TokenStatus.WAITING;
        this.issuedAt = LocalDateTime.now();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void activate(int activeDurationMinutes) {
        if (tokenStatus != TokenStatus.WAITING) {
            throw new IllegalStateException("Only waiting tokens can be activated");
        }

        this.tokenStatus = TokenStatus.ACTIVE;
        this.activatedAt = LocalDateTime.now();
        this.expiresAt = LocalDateTime.now().plusMinutes(activeDurationMinutes);
    }

    public void expire() {
        this.tokenStatus = TokenStatus.EXPIRED;
    }

    public void complete() {
        this.tokenStatus = TokenStatus.COMPLETED;
    }

    public boolean isActive() {
        if (tokenStatus != TokenStatus.ACTIVE) {
            return false;
        }

        // 만료 시간이 지났으면 자동으로 만료 처리
        if (expiresAt != null && LocalDateTime.now().isAfter(expiresAt)) {
            expire();
            return false;
        }

        return true;
    }

    public boolean isWaiting() {
        return tokenStatus == TokenStatus.WAITING;
    }

    public boolean isExpired() {
        return tokenStatus == TokenStatus.EXPIRED ||
               (expiresAt != null && LocalDateTime.now().isAfter(expiresAt));
    }

    public long getEstimatedWaitTimeMinutes() {
        if (tokenStatus == TokenStatus.ACTIVE) {
            return 0;
        }

        if (tokenStatus != TokenStatus.WAITING) {
            return -1; // 대기 상태가 아님
        }

        // 대기 순서에 따른 예상 대기 시간 계산 (1분당 10명씩 처리 가정)
        return Math.max(0, (queuePosition - 1) / 10);
    }

    public void updatePosition(Long newPosition) {
        this.queuePosition = newPosition;
    }

    public Long getTokenId() {
        return tokenId;
    }

    public Long getUserId() {
        return userId;
    }

    public String getTokenUuid() {
        return tokenUuid;
    }

    public TokenStatus getTokenStatus() {
        return tokenStatus;
    }

    public Long getQueuePosition() {
        return queuePosition;
    }

    public LocalDateTime getIssuedAt() {
        return issuedAt;
    }

    public LocalDateTime getActivatedAt() {
        return activatedAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public enum TokenStatus {
        WAITING,    // 대기 중
        ACTIVE,     // 활성화됨 (예약 가능)
        EXPIRED,    // 만료됨
        COMPLETED   // 완료됨 (예약/결제 완료)
    }
}