package kr.hhplus.be.server.domain.concert;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "reservations")
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reservation_id")
    private Long reservationId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;

    @Enumerated(EnumType.STRING)
    @Column(name = "reservation_status", nullable = false, length = 20)
    private ReservationStatus reservationStatus = ReservationStatus.TEMPORARY;

    @Column(name = "price", nullable = false, precision = 15, scale = 2)
    private BigDecimal price;

    @Column(name = "reserved_at", nullable = false)
    private LocalDateTime reservedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected Reservation() {
    }

    public Reservation(Long userId, Seat seat, BigDecimal price) {
        this.userId = userId;
        this.seat = seat;
        this.price = price;
        this.reservationStatus = ReservationStatus.TEMPORARY;
        this.reservedAt = LocalDateTime.now();
        this.expiresAt = LocalDateTime.now().plusMinutes(5); // 5분 임시 예약
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void confirm() {
        if (reservationStatus != ReservationStatus.TEMPORARY) {
            throw new IllegalStateException("Only temporary reservations can be confirmed");
        }

        if (isExpired()) {
            throw new IllegalStateException("Reservation has expired");
        }

        this.reservationStatus = ReservationStatus.CONFIRMED;
        this.confirmedAt = LocalDateTime.now();
        this.expiresAt = null; // 확정 예약 시 만료 시간 제거
    }

    public void cancel() {
        this.reservationStatus = ReservationStatus.CANCELLED;
    }

    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isTemporary() {
        return reservationStatus == ReservationStatus.TEMPORARY;
    }

    public boolean isConfirmed() {
        return reservationStatus == ReservationStatus.CONFIRMED;
    }

    public Long getReservationId() {
        return reservationId;
    }

    public Long getUserId() {
        return userId;
    }

    public Seat getSeat() {
        return seat;
    }

    public ReservationStatus getReservationStatus() {
        return reservationStatus;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public LocalDateTime getReservedAt() {
        return reservedAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public LocalDateTime getConfirmedAt() {
        return confirmedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public enum ReservationStatus {
        TEMPORARY,   // 임시 예약 (5분간)
        CONFIRMED,   // 확정 예약 (결제 완료)
        CANCELLED    // 취소됨
    }
}