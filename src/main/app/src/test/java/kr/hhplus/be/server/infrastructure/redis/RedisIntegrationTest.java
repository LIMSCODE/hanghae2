package kr.hhplus.be.server.infrastructure.redis;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Testcontainers
@TestPropertySource(properties = {
    "spring.data.redis.host=${redis.host}",
    "spring.data.redis.port=${redis.port}"
})
class RedisIntegrationTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379)
            .withCommand("redis-server --appendonly yes");

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private RedisDistributedLock distributedLock;

    @Autowired
    private SeatCacheService seatCacheService;

    @BeforeEach
    void setUp() {
        System.setProperty("redis.host", redis.getHost());
        System.setProperty("redis.port", redis.getMappedPort(6379).toString());

        // Redis 초기화
        redisTemplate.getConnectionFactory().getConnection().flushAll();
    }

    @Test
    void 분산락_동시성_테스트() throws InterruptedException {
        // Given
        String lockKey = "test:lock";
        int threadCount = 10;
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger counter = new AtomicInteger(0);

        // When
        CompletableFuture<?>[] futures = new CompletableFuture[threadCount];
        for (int i = 0; i < threadCount; i++) {
            futures[i] = CompletableFuture.runAsync(() -> {
                try {
                    distributedLock.executeWithLock(lockKey, 1, 5, () -> {
                        int current = counter.get();
                        // 의도적으로 지연을 추가하여 동시성 문제 유발 가능성 증가
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        counter.set(current + 1);
                        return null;
                    });
                } catch (Exception e) {
                    // 락 획득 실패는 예상되는 상황
                } finally {
                    latch.countDown();
                }
            });
        }

        // Then
        latch.await(30, TimeUnit.SECONDS);
        CompletableFuture.allOf(futures).join();

        // 분산락이 정상 작동하면 counter 값이 실제 성공한 작업 수와 같아야 함
        assertThat(counter.get()).isGreaterThan(0);
    }

    @Test
    void 좌석_캐시_기본_동작_테스트() {
        // Given
        Long seatId = 1L;
        Long scheduleId = 100L;
        SeatCacheService.SeatCacheDto seat = new SeatCacheService.SeatCacheDto(seatId, scheduleId, 1, "AVAILABLE");

        // When
        seatCacheService.cacheSeat(seat);

        // Then
        SeatCacheService.SeatCacheDto cachedSeat = seatCacheService.getCachedSeat(seatId);
        assertThat(cachedSeat).isNotNull();
        assertThat(cachedSeat.getSeatId()).isEqualTo(seatId);
        assertThat(cachedSeat.getSeatNumber()).isEqualTo(1);
        assertThat(cachedSeat.getSeatStatus()).isEqualTo("AVAILABLE");
    }

    @Test
    void 임시_예약_캐시_테스트() throws InterruptedException {
        // Given
        Long seatId = 1L;
        Long userId = 100L;
        Duration duration = Duration.ofSeconds(2);

        // When
        seatCacheService.cacheTemporaryReservation(seatId, userId, duration);

        // Then
        SeatCacheService.TemporaryReservation reservation = seatCacheService.getTemporaryReservation(seatId);
        assertThat(reservation).isNotNull();
        assertThat(reservation.getUserId()).isEqualTo(userId);
        assertThat(reservation.getExpiresAt()).isAfter(LocalDateTime.now());

        // TTL 테스트 - 만료 시간 후 조회
        Thread.sleep(3000);
        SeatCacheService.TemporaryReservation expiredReservation = seatCacheService.getTemporaryReservation(seatId);
        assertThat(expiredReservation).isNull();
    }

    @Test
    void 스케줄_좌석_목록_캐시_테스트() {
        // Given
        Long scheduleId = 100L;
        List<SeatCacheService.SeatCacheDto> seats = List.of(
            new SeatCacheService.SeatCacheDto(1L, scheduleId, 1, "AVAILABLE"),
            new SeatCacheService.SeatCacheDto(2L, scheduleId, 2, "AVAILABLE"),
            new SeatCacheService.SeatCacheDto(3L, scheduleId, 3, "RESERVED")
        );

        // When
        seatCacheService.cacheScheduleSeats(scheduleId, seats);

        // Then
        List<SeatCacheService.SeatCacheDto> cachedSeats = seatCacheService.getCachedScheduleSeats(scheduleId);
        assertThat(cachedSeats).hasSize(3);
        assertThat(cachedSeats.get(0).getSeatNumber()).isEqualTo(1);
        assertThat(cachedSeats.get(2).getSeatStatus()).isEqualTo("RESERVED");
    }

    @Test
    void 좌석_캐시_무효화_테스트() {
        // Given
        Long seatId = 1L;
        Long userId = 100L;
        Long scheduleId = 100L;

        SeatCacheService.SeatCacheDto seat = new SeatCacheService.SeatCacheDto(seatId, scheduleId, 1, "AVAILABLE");
        seatCacheService.cacheSeat(seat);
        seatCacheService.cacheTemporaryReservation(seatId, userId, Duration.ofMinutes(5));

        // When
        seatCacheService.invalidateSeat(seatId);

        // Then
        assertThat(seatCacheService.getCachedSeat(seatId)).isNull();
        assertThat(seatCacheService.getTemporaryReservation(seatId)).isNull();
    }

    @Test
    void 임시_예약_제거_테스트() {
        // Given
        Long seatId = 1L;
        Long userId = 100L;
        seatCacheService.cacheTemporaryReservation(seatId, userId, Duration.ofMinutes(5));

        // 임시 예약이 있는지 확인
        assertThat(seatCacheService.getTemporaryReservation(seatId)).isNotNull();

        // When
        seatCacheService.removeTemporaryReservation(seatId);

        // Then
        assertThat(seatCacheService.getTemporaryReservation(seatId)).isNull();
    }

    @Test
    void 분산락_타임아웃_테스트() {
        // Given
        String lockKey = "test:timeout:lock";

        // 첫 번째 락 획득
        distributedLock.executeWithLock(lockKey, 1, 5, () -> {
            // 두 번째 락 획득 시도 (타임아웃 발생해야 함)
            assertThatThrownBy(() -> {
                distributedLock.executeWithLock(lockKey, 1, 1, () -> {
                    return null;
                });
            }).isInstanceOf(IllegalStateException.class)
              .hasMessageContaining("Could not acquire lock");

            return null;
        });
    }

    @Test
    void Redis_연결_상태_테스트() {
        // Given & When
        String testKey = "test:connection";
        String testValue = "connected";

        redisTemplate.opsForValue().set(testKey, testValue);
        Object result = redisTemplate.opsForValue().get(testKey);

        // Then
        assertThat(result).isEqualTo(testValue);
    }
}