package kr.hhplus.be.server.domain.concert;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "concert_schedules")
public class ConcertSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "schedule_id")
    private Long scheduleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "concert_id", nullable = false)
    private Concert concert;

    @Column(name = "concert_date", nullable = false)
    private LocalDateTime concertDate;

    @Column(name = "reservation_open_at", nullable = false)
    private LocalDateTime reservationOpenAt;

    @Column(name = "total_seats", nullable = false)
    private Integer totalSeats = 50; // 기본값 50석

    @Column(name = "available_seats", nullable = false)
    private Integer availableSeats = 50;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "schedule", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Seat> seats = new ArrayList<>();

    protected ConcertSchedule() {
    }

    public ConcertSchedule(Concert concert, LocalDateTime concertDate, LocalDateTime reservationOpenAt) {
        this.concert = concert;
        this.concertDate = concertDate;
        this.reservationOpenAt = reservationOpenAt;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        initializeSeats();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    private void initializeSeats() {
        for (int seatNumber = 1; seatNumber <= totalSeats; seatNumber++) {
            seats.add(new Seat(this, seatNumber));
        }
    }

    public boolean isReservationOpen() {
        return LocalDateTime.now().isAfter(reservationOpenAt);
    }

    public boolean hasAvailableSeats() {
        return availableSeats > 0;
    }

    public void decreaseAvailableSeats() {
        if (availableSeats > 0) {
            this.availableSeats--;
        }
    }

    public void increaseAvailableSeats() {
        if (availableSeats < totalSeats) {
            this.availableSeats++;
        }
    }

    public Long getScheduleId() {
        return scheduleId;
    }

    public Concert getConcert() {
        return concert;
    }

    public void setConcert(Concert concert) {
        this.concert = concert;
    }

    public LocalDateTime getConcertDate() {
        return concertDate;
    }

    public LocalDateTime getReservationOpenAt() {
        return reservationOpenAt;
    }

    public Integer getTotalSeats() {
        return totalSeats;
    }

    public Integer getAvailableSeats() {
        return availableSeats;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public List<Seat> getSeats() {
        return seats;
    }
}