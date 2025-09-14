package kr.hhplus.be.server.integration;

import kr.hhplus.be.server.domain.concert.*;
import kr.hhplus.be.server.domain.queue.QueueToken;
import kr.hhplus.be.server.infrastructure.concert.JpaReservationRepository;
import kr.hhplus.be.server.infrastructure.concert.JpaSeatRepository;
import kr.hhplus.be.server.infrastructure.queue.JpaQueueTokenRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class ConcertReservationIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private JpaSeatRepository seatRepository;

    @Autowired
    private JpaReservationRepository reservationRepository;

    @Autowired
    private JpaQueueTokenRepository queueTokenRepository;

    @Test
    @DisplayName("콘서트 예약 전체 플로우 통합 테스트")
    void concertReservationFullFlow() {
        // Given: 콘서트 및 스케줄 설정
        Concert concert = new Concert("Spring Concert", "Spring Band", "Spring Hall");
        entityManager.persist(concert);

        ConcertSchedule schedule = new ConcertSchedule(
                concert,
                LocalDateTime.now().plusDays(7),
                LocalDateTime.now().minusHours(1)
        );
        entityManager.persist(schedule);

        Seat seat = new Seat(schedule, 1);
        entityManager.persist(seat);

        entityManager.flush();
        entityManager.clear();

        Long userId = 1L;
        BigDecimal price = new BigDecimal("50000");

        // 1. 대기열 토큰 생성
        QueueToken token = new QueueToken(userId, 1L);
        token.activate(10);
        queueTokenRepository.save(token);

        // 2. 좌석 예약
        Seat foundSeat = seatRepository.findById(seat.getSeatId()).orElseThrow();
        assertThat(foundSeat.isAvailable()).isTrue();

        foundSeat.reserve(userId, 5);
        seatRepository.save(foundSeat);

        // 3. 예약 정보 생성
        Reservation reservation = new Reservation(userId, foundSeat, price);
        reservationRepository.save(reservation);

        // 4. 예약 확정
        reservation.confirm();
        foundSeat.confirmReservation();

        reservationRepository.save(reservation);
        seatRepository.save(foundSeat);

        // 5. 토큰 완료 처리
        token.complete();
        queueTokenRepository.save(token);

        // Then: 최종 상태 검증
        Seat finalSeat = seatRepository.findById(seat.getSeatId()).orElseThrow();
        Reservation finalReservation = reservationRepository.findById(reservation.getReservationId()).orElseThrow();
        QueueToken finalToken = queueTokenRepository.findById(token.getTokenId()).orElseThrow();

        assertThat(finalSeat.getSeatStatus()).isEqualTo(Seat.SeatStatus.RESERVED);
        assertThat(finalSeat.getReservedUserId()).isEqualTo(userId);

        assertThat(finalReservation.getReservationStatus()).isEqualTo(Reservation.ReservationStatus.CONFIRMED);
        assertThat(finalReservation.getConfirmedAt()).isNotNull();

        assertThat(finalToken.getTokenStatus()).isEqualTo(QueueToken.TokenStatus.COMPLETED);
    }

    @Test
    @DisplayName("동시성 테스트 - 여러 사용자가 동시에 같은 좌석 예약 시도")
    void concurrentSeatReservationTest() throws InterruptedException {
        // Given
        Concert concert = new Concert("Concurrent Test Concert", "Test Band", "Test Hall");
        entityManager.persist(concert);

        ConcertSchedule schedule = new ConcertSchedule(
                concert,
                LocalDateTime.now().plusDays(7),
                LocalDateTime.now().minusHours(1)
        );
        entityManager.persist(schedule);

        Seat seat = new Seat(schedule, 1);
        entityManager.persist(seat);

        entityManager.flush();
        entityManager.clear();

        int threadCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // When: 동시에 좌석 예약 시도
        for (int i = 0; i < threadCount; i++) {
            final long userId = i + 1;
            executorService.submit(() -> {
                try {
                    Seat foundSeat = seatRepository.findById(seat.getSeatId()).orElseThrow();

                    if (foundSeat.isAvailable()) {
                        foundSeat.reserve(userId, 5);
                        seatRepository.save(foundSeat);

                        Reservation reservation = new Reservation(userId, foundSeat, new BigDecimal("50000"));
                        reservationRepository.save(reservation);

                        successCount.incrementAndGet();
                    } else {
                        failCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // Then: 오직 한 명만 성공해야 함
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failCount.get()).isEqualTo(threadCount - 1);

        // 최종 좌석 상태 확인
        Seat finalSeat = seatRepository.findById(seat.getSeatId()).orElseThrow();
        assertThat(finalSeat.getSeatStatus()).isEqualTo(Seat.SeatStatus.TEMPORARY_RESERVED);
        assertThat(finalSeat.getReservedUserId()).isNotNull();
    }

    @Test
    @DisplayName("대기열 토큰 관리 통합 테스트")
    void queueTokenManagementIntegrationTest() {
        // Given: 여러 사용자의 토큰 생성
        QueueToken token1 = new QueueToken(1L, 1L);
        QueueToken token2 = new QueueToken(2L, 2L);
        QueueToken token3 = new QueueToken(3L, 3L);

        queueTokenRepository.save(token1);
        queueTokenRepository.save(token2);
        queueTokenRepository.save(token3);

        // When: 첫 번째 토큰 활성화
        token1.activate(10);
        queueTokenRepository.save(token1);

        // Then: 대기열 상태 확인
        List<QueueToken> waitingTokens = queueTokenRepository.findWaitingTokens();
        List<QueueToken> activeTokens = queueTokenRepository.findActiveTokens();
        Long waitingCount = queueTokenRepository.countWaitingTokens();

        assertThat(activeTokens).hasSize(1);
        assertThat(activeTokens.get(0).getUserId()).isEqualTo(1L);

        assertThat(waitingTokens).hasSize(2);
        assertThat(waitingCount).isEqualTo(2L);

        // When: 활성화된 토큰 완료 처리
        token1.complete();
        queueTokenRepository.save(token1);

        // Then: 완료된 토큰은 활성화 목록에서 제외
        List<QueueToken> updatedActiveTokens = queueTokenRepository.findActiveTokens();
        assertThat(updatedActiveTokens).isEmpty();
    }

    @Test
    @DisplayName("예약 만료 처리 통합 테스트")
    void reservationExpirationIntegrationTest() throws InterruptedException {
        // Given: 만료될 예약 생성
        Concert concert = new Concert("Expiration Test Concert", "Test Band", "Test Hall");
        entityManager.persist(concert);

        ConcertSchedule schedule = new ConcertSchedule(
                concert,
                LocalDateTime.now().plusDays(7),
                LocalDateTime.now().minusHours(1)
        );
        entityManager.persist(schedule);

        Seat seat = new Seat(schedule, 1);
        entityManager.persist(seat);

        entityManager.flush();
        entityManager.clear();

        // 좌석 임시 예약 (매우 짧은 시간)
        Seat foundSeat = seatRepository.findById(seat.getSeatId()).orElseThrow();
        foundSeat.reserve(1L, 0); // 0분으로 설정하여 즉시 만료되도록

        Reservation reservation = new Reservation(1L, foundSeat, new BigDecimal("50000"));

        seatRepository.save(foundSeat);
        reservationRepository.save(reservation);

        // 약간의 시간 대기
        Thread.sleep(100);

        // When: 만료된 예약 조회
        List<Reservation> expiredReservations = reservationRepository.findExpiredTemporaryReservations();

        // Then: 만료된 예약이 조회되어야 함
        assertThat(expiredReservations).isNotEmpty();

        // 좌석 상태도 만료 확인 시 해제되어야 함
        Seat expiredSeat = seatRepository.findById(seat.getSeatId()).orElseThrow();
        assertThat(expiredSeat.isExpired()).isTrue();
        assertThat(expiredSeat.isAvailable()).isTrue(); // 만료 시 자동으로 사용 가능 상태로 변경
    }
}