package kr.hhplus.be.server.domain;

import jakarta.persistence.*;
import kr.hhplus.be.server.exception.BusinessException;
import kr.hhplus.be.server.exception.ErrorCode;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "queue")
public class Queue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long queueId;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QueueStatus status;

    @Column(nullable = false)
    private Integer position;

    @Column(nullable = false, updatable = false)
    private LocalDateTime enteredAt;

    private LocalDateTime activatedAt;

    private LocalDateTime expiredAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public enum QueueStatus {
        WAITING,    // 대기중
        ACTIVE,     // 활성화 (입장 가능)
        EXPIRED     // 만료됨
    }

    protected Queue() {}

    public Queue(Long userId) {
        this.token = UUID.randomUUID().toString();
        this.userId = userId;
        this.status = QueueStatus.WAITING;
        this.position = 0; // 실제 포지션은 서비스에서 설정
        this.enteredAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void activate() {
        if (this.status != QueueStatus.WAITING) {
            throw new BusinessException(ErrorCode.INVALID_QUEUE_STATUS, "대기중인 상태가 아닙니다");
        }

        this.status = QueueStatus.ACTIVE;
        this.activatedAt = LocalDateTime.now();
        this.expiredAt = LocalDateTime.now().plusMinutes(10); // 10분 후 만료
        this.position = 0;
        this.updatedAt = LocalDateTime.now();
    }

    public void expire() {
        this.status = QueueStatus.EXPIRED;
        this.expiredAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isExpired() {
        return status == QueueStatus.EXPIRED ||
               (expiredAt != null && LocalDateTime.now().isAfter(expiredAt));
    }

    public boolean isActive() {
        return status == QueueStatus.ACTIVE && !isExpired();
    }

    public boolean isWaiting() {
        return status == QueueStatus.WAITING;
    }

    public void updatePosition(Integer newPosition) {
        if (status == QueueStatus.WAITING) {
            this.position = newPosition;
            this.updatedAt = LocalDateTime.now();
        }
    }

    // Getters
    public Long getQueueId() { return queueId; }
    public String getToken() { return token; }
    public Long getUserId() { return userId; }
    public QueueStatus getStatus() { return status; }
    public Integer getPosition() { return position; }
    public LocalDateTime getEnteredAt() { return enteredAt; }
    public LocalDateTime getActivatedAt() { return activatedAt; }
    public LocalDateTime getExpiredAt() { return expiredAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    // Setters for JPA
    public void setQueueId(Long queueId) { this.queueId = queueId; }
    public void setToken(String token) { this.token = token; }
    public void setUserId(Long userId) { this.userId = userId; }
    public void setStatus(QueueStatus status) { this.status = status; }
    public void setPosition(Integer position) { this.position = position; }
    public void setEnteredAt(LocalDateTime enteredAt) { this.enteredAt = enteredAt; }
    public void setActivatedAt(LocalDateTime activatedAt) { this.activatedAt = activatedAt; }
    public void setExpiredAt(LocalDateTime expiredAt) { this.expiredAt = expiredAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}