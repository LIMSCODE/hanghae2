package kr.hhplus.be.server.domain.concert;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "seats")
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "seat_id")
    private Long seatId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private ConcertSchedule schedule;

    @Column(name = "seat_number", nullable = false)
    private Integer seatNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "seat_status", nullable = false, length = 20)
    private SeatStatus seatStatus = SeatStatus.AVAILABLE;

    @Column(name = "reserved_user_id")
    private Long reservedUserId;

    @Column(name = "reserved_at")
    private LocalDateTime reservedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected Seat() {
    }

    public Seat(ConcertSchedule schedule, Integer seatNumber) {
        this.schedule = schedule;
        this.seatNumber = seatNumber;
        this.seatStatus = SeatStatus.AVAILABLE;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void reserve(Long userId, int temporaryReservationMinutes) {
        if (!isAvailable()) {
            throw new IllegalStateException("Seat is not available for reservation");
        }

        this.seatStatus = SeatStatus.TEMPORARY_RESERVED;
        this.reservedUserId = userId;
        this.reservedAt = LocalDateTime.now();
        this.expiresAt = LocalDateTime.now().plusMinutes(temporaryReservationMinutes);
    }

    public void confirmReservation() {
        if (seatStatus != SeatStatus.TEMPORARY_RESERVED) {
            throw new IllegalStateException("Seat is not temporarily reserved");
        }

        this.seatStatus = SeatStatus.RESERVED;
        this.expiresAt = null; // 확정 예약 시 만료 시간 제거
    }

    public void release() {
        this.seatStatus = SeatStatus.AVAILABLE;
        this.reservedUserId = null;
        this.reservedAt = null;
        this.expiresAt = null;
    }

    public boolean isAvailable() {
        if (seatStatus == SeatStatus.AVAILABLE) {
            return true;
        }

        // 임시 예약이 만료된 경우 자동으로 해제
        if (seatStatus == SeatStatus.TEMPORARY_RESERVED && isExpired()) {
            release();
            return true;
        }

        return false;
    }

    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isReservedBy(Long userId) {
        return this.reservedUserId != null && this.reservedUserId.equals(userId);
    }

    public Long getSeatId() {
        return seatId;
    }

    public ConcertSchedule getSchedule() {
        return schedule;
    }

    public Integer getSeatNumber() {
        return seatNumber;
    }

    public SeatStatus getSeatStatus() {
        return seatStatus;
    }

    public Long getReservedUserId() {
        return reservedUserId;
    }

    public LocalDateTime getReservedAt() {
        return reservedAt;
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

    public enum SeatStatus {
        AVAILABLE,           // 예약 가능
        TEMPORARY_RESERVED,  // 임시 예약 (5분간)
        RESERVED             // 확정 예약
    }
}