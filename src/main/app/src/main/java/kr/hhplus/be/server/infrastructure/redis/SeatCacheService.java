package kr.hhplus.be.server.infrastructure.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class SeatCacheService {

    private static final Logger log = LoggerFactory.getLogger(SeatCacheService.class);
    private static final String SEAT_LAYOUT_PREFIX = "seat:layout:";
    private static final String POPULAR_CONCERTS_KEY = "concerts:popular";
    private static final Duration LAYOUT_CACHE_EXPIRY = Duration.ofHours(2); // 2시간 캐시 (좌석 배치는 자주 안 바뀜)
    private static final Duration POPULAR_CACHE_EXPIRY = Duration.ofMinutes(15); // 15분 캐시

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public SeatCacheService(RedisTemplate<String, Object> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 좌석 배치도 및 가격 정보 캐시 저장
     */
    public void cacheSeatLayout(Long scheduleId, List<SeatLayoutDto> seatLayout) {
        try {
            String key = SEAT_LAYOUT_PREFIX + scheduleId;
            redisTemplate.opsForValue().set(key, seatLayout, LAYOUT_CACHE_EXPIRY);
            log.debug("Cached seat layout for schedule: {} with {} seats", scheduleId, seatLayout.size());
        } catch (Exception e) {
            log.error("Failed to cache seat layout for schedule: {}", scheduleId, e);
        }
    }

    /**
     * 좌석 배치도 조회 (캐시 우선)
     */
    @SuppressWarnings("unchecked")
    public List<SeatLayoutDto> getCachedSeatLayout(Long scheduleId) {
        try {
            String key = SEAT_LAYOUT_PREFIX + scheduleId;
            Object cached = redisTemplate.opsForValue().get(key);

            if (cached instanceof List) {
                log.debug("Cache hit for seat layout: {}", scheduleId);
                return (List<SeatLayoutDto>) cached;
            }

            log.debug("Cache miss for seat layout: {}", scheduleId);
            return null;
        } catch (Exception e) {
            log.error("Failed to get cached seat layout for schedule: {}", scheduleId, e);
            return null;
        }
    }

    /**
     * 인기 콘서트 목록 캐시 저장
     */
    public void cachePopularConcerts(List<PopularConcertDto> popularConcerts) {
        try {
            redisTemplate.opsForValue().set(POPULAR_CONCERTS_KEY, popularConcerts, POPULAR_CACHE_EXPIRY);
            log.debug("Cached {} popular concerts", popularConcerts.size());
        } catch (Exception e) {
            log.error("Failed to cache popular concerts", e);
        }
    }

    /**
     * 인기 콘서트 목록 조회 (캐시 우선)
     */
    @SuppressWarnings("unchecked")
    public List<PopularConcertDto> getCachedPopularConcerts() {
        try {
            Object cached = redisTemplate.opsForValue().get(POPULAR_CONCERTS_KEY);

            if (cached instanceof List) {
                log.debug("Cache hit for popular concerts");
                return (List<PopularConcertDto>) cached;
            }

            log.debug("Cache miss for popular concerts");
            return null;
        } catch (Exception e) {
            log.error("Failed to get cached popular concerts", e);
            return null;
        }
    }

    /**
     * 좌석 배치도 캐시 무효화 (좌석 가격/배치 변경 시)
     */
    public void invalidateSeatLayout(Long scheduleId) {
        try {
            String key = SEAT_LAYOUT_PREFIX + scheduleId;
            redisTemplate.delete(key);
            log.debug("Invalidated seat layout cache for schedule: {}", scheduleId);
        } catch (Exception e) {
            log.error("Failed to invalidate seat layout cache for schedule: {}", scheduleId, e);
        }
    }

    /**
     * 인기 콘서트 캐시 무효화
     */
    public void invalidatePopularConcerts() {
        try {
            redisTemplate.delete(POPULAR_CONCERTS_KEY);
            log.debug("Invalidated popular concerts cache");
        } catch (Exception e) {
            log.error("Failed to invalidate popular concerts cache", e);
        }
    }

    // DTO 클래스들
    public static class SeatLayoutDto {
        private Long seatId;
        private Integer seatNumber;
        private String seatGrade; // VIP, R석, S석 등
        private BigDecimal price;
        private Integer rowNumber;
        private Integer columnNumber;
        private boolean isAvailable;

        public SeatLayoutDto() {}

        public SeatLayoutDto(Long seatId, Integer seatNumber, String seatGrade, BigDecimal price,
                           Integer rowNumber, Integer columnNumber, boolean isAvailable) {
            this.seatId = seatId;
            this.seatNumber = seatNumber;
            this.seatGrade = seatGrade;
            this.price = price;
            this.rowNumber = rowNumber;
            this.columnNumber = columnNumber;
            this.isAvailable = isAvailable;
        }

        // Getters and setters
        public Long getSeatId() { return seatId; }
        public void setSeatId(Long seatId) { this.seatId = seatId; }

        public Integer getSeatNumber() { return seatNumber; }
        public void setSeatNumber(Integer seatNumber) { this.seatNumber = seatNumber; }

        public String getSeatGrade() { return seatGrade; }
        public void setSeatGrade(String seatGrade) { this.seatGrade = seatGrade; }

        public BigDecimal getPrice() { return price; }
        public void setPrice(BigDecimal price) { this.price = price; }

        public Integer getRowNumber() { return rowNumber; }
        public void setRowNumber(Integer rowNumber) { this.rowNumber = rowNumber; }

        public Integer getColumnNumber() { return columnNumber; }
        public void setColumnNumber(Integer columnNumber) { this.columnNumber = columnNumber; }

        public boolean isAvailable() { return isAvailable; }
        public void setAvailable(boolean available) { isAvailable = available; }
    }

    public static class PopularConcertDto {
        private Long concertId;
        private String title;
        private String artist;
        private LocalDateTime startDate;
        private LocalDateTime endDate;
        private Integer totalSeats;
        private Integer bookedSeats;
        private Double bookingRate;

        public PopularConcertDto() {}

        public PopularConcertDto(Long concertId, String title, String artist,
                               LocalDateTime startDate, LocalDateTime endDate,
                               Integer totalSeats, Integer bookedSeats, Double bookingRate) {
            this.concertId = concertId;
            this.title = title;
            this.artist = artist;
            this.startDate = startDate;
            this.endDate = endDate;
            this.totalSeats = totalSeats;
            this.bookedSeats = bookedSeats;
            this.bookingRate = bookingRate;
        }

        // Getters and setters
        public Long getConcertId() { return concertId; }
        public void setConcertId(Long concertId) { this.concertId = concertId; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getArtist() { return artist; }
        public void setArtist(String artist) { this.artist = artist; }

        public LocalDateTime getStartDate() { return startDate; }
        public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }

        public LocalDateTime getEndDate() { return endDate; }
        public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }

        public Integer getTotalSeats() { return totalSeats; }
        public void setTotalSeats(Integer totalSeats) { this.totalSeats = totalSeats; }

        public Integer getBookedSeats() { return bookedSeats; }
        public void setBookedSeats(Integer bookedSeats) { this.bookedSeats = bookedSeats; }

        public Double getBookingRate() { return bookingRate; }
        public void setBookingRate(Double bookingRate) { this.bookingRate = bookingRate; }
    }
}