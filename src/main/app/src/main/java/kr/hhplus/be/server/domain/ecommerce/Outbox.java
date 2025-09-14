package kr.hhplus.be.server.domain.ecommerce;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "outbox")
public class Outbox {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "outbox_id")
    private Long outboxId;

    @Column(name = "aggregate_id", nullable = false)
    private String aggregateId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OutboxStatus status = OutboxStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    protected Outbox() {
    }

    public Outbox(String aggregateId, String eventType, String payload) {
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
        this.status = OutboxStatus.PENDING;
        this.createdAt = LocalDateTime.now();
        this.retryCount = 0;
    }

    public void markAsProcessed() {
        this.status = OutboxStatus.PROCESSED;
        this.processedAt = LocalDateTime.now();
    }

    public void markAsFailed(String errorMessage) {
        this.status = OutboxStatus.FAILED;
        this.errorMessage = errorMessage;
        this.retryCount++;
    }

    public void retry() {
        if (this.retryCount < 3) { // 최대 3회 재시도
            this.status = OutboxStatus.PENDING;
            this.errorMessage = null;
        }
    }

    public boolean canRetry() {
        return this.retryCount < 3 && this.status == OutboxStatus.FAILED;
    }

    // Getters
    public Long getOutboxId() { return outboxId; }
    public String getAggregateId() { return aggregateId; }
    public String getEventType() { return eventType; }
    public String getPayload() { return payload; }
    public OutboxStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getProcessedAt() { return processedAt; }
    public Integer getRetryCount() { return retryCount; }
    public String getErrorMessage() { return errorMessage; }

    public enum OutboxStatus {
        PENDING,    // 처리 대기중
        PROCESSED,  // 처리 완료
        FAILED      // 처리 실패
    }
}