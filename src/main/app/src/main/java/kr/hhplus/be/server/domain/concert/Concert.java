package kr.hhplus.be.server.domain.concert;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "concerts")
public class Concert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "concert_id")
    private Long concertId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "artist", nullable = false, length = 100)
    private String artist;

    @Column(name = "venue", nullable = false, length = 200)
    private String venue;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "concert", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ConcertSchedule> schedules = new ArrayList<>();

    protected Concert() {
    }

    public Concert(String title, String artist, String venue) {
        this.title = title;
        this.artist = artist;
        this.venue = venue;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void addSchedule(ConcertSchedule schedule) {
        this.schedules.add(schedule);
        schedule.setConcert(this);
    }

    public Long getConcertId() {
        return concertId;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public String getVenue() {
        return venue;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public List<ConcertSchedule> getSchedules() {
        return schedules;
    }
}