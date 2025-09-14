package kr.hhplus.be.server.dto;

import kr.hhplus.be.server.domain.Queue;

import java.time.LocalDateTime;

public class QueueResponse {

    private String token;
    private Long userId;
    private String status;
    private Integer position;
    private LocalDateTime enteredAt;
    private LocalDateTime activatedAt;
    private LocalDateTime expiredAt;
    private Integer estimatedWaitTimeMinutes;

    public QueueResponse(Queue queue) {
        this.token = queue.getToken();
        this.userId = queue.getUserId();
        this.status = queue.getStatus().name();
        this.position = queue.getPosition();
        this.enteredAt = queue.getEnteredAt();
        this.activatedAt = queue.getActivatedAt();
        this.expiredAt = queue.getExpiredAt();

        // 예상 대기 시간 계산 (대기 순번 * 평균 처리 시간)
        if (queue.isWaiting() && queue.getPosition() != null) {
            this.estimatedWaitTimeMinutes = queue.getPosition() * 2; // 가정: 사용자당 2분
        }
    }

    // Getters
    public String getToken() { return token; }
    public Long getUserId() { return userId; }
    public String getStatus() { return status; }
    public Integer getPosition() { return position; }
    public LocalDateTime getEnteredAt() { return enteredAt; }
    public LocalDateTime getActivatedAt() { return activatedAt; }
    public LocalDateTime getExpiredAt() { return expiredAt; }
    public Integer getEstimatedWaitTimeMinutes() { return estimatedWaitTimeMinutes; }
}