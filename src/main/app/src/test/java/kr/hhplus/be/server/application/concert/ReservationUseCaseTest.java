package kr.hhplus.be.server.application.concert;

import kr.hhplus.be.server.domain.concert.Reservation;
import kr.hhplus.be.server.domain.concert.Seat;
import kr.hhplus.be.server.domain.concert.ConcertSchedule;
import kr.hhplus.be.server.domain.concert.Concert;
import kr.hhplus.be.server.domain.queue.QueueToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationUseCaseTest {

    @Mock
    private SeatRepository seatRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private QueueTokenRepository queueTokenRepository;

    @Mock
    private UserBalanceService userBalanceService;

    @Mock
    private PaymentService paymentService;

    private ReservationUseCase reservationUseCase;

    @BeforeEach
    void setUp() {
        reservationUseCase = new ReservationUseCase(
                seatRepository,
                reservationRepository,
                queueTokenRepository,
                userBalanceService,
                paymentService
        );
    }

    @Test
    @DisplayName("좌석 예약 성공 - 유효한 토큰과 사용 가능한 좌석으로 예약")
    void reserveSeat_Success() {
        // Given
        String tokenUuid = "test-token-uuid";
        Long userId = 1L;
        Long seatId = 1L;
        BigDecimal price = new BigDecimal("50000");

        QueueToken activeToken = new QueueToken(userId, 1L);
        activeToken.activate(10);

        Concert concert = new Concert("Test Concert", "Test Artist", "Test Venue");
        ConcertSchedule schedule = new ConcertSchedule(concert, LocalDateTime.now().plusDays(1), LocalDateTime.now());
        Seat availableSeat = new Seat(schedule, 1);

        when(queueTokenRepository.findByTokenUuid(tokenUuid)).thenReturn(Optional.of(activeToken));
        when(seatRepository.findById(seatId)).thenReturn(Optional.of(availableSeat));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(seatRepository.save(any(Seat.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReservationUseCase.ReserveSeatCommand command = new ReservationUseCase.ReserveSeatCommand(
                tokenUuid, userId, seatId, price
        );

        // When
        ReservationUseCase.ReservationResult result = reservationUseCase.reserveSeat(command);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getSeatNumber()).isEqualTo(1);
        assertThat(result.getPrice()).isEqualTo(price);
        assertThat(result.getExpiresAt()).isAfter(LocalDateTime.now());

        verify(seatRepository).save(any(Seat.class));
        verify(reservationRepository).save(any(Reservation.class));
    }

    @Test
    @DisplayName("좌석 예약 실패 - 유효하지 않은 토큰")
    void reserveSeat_InvalidToken_ShouldThrowException() {
        // Given
        String invalidTokenUuid = "invalid-token";
        Long userId = 1L;
        Long seatId = 1L;
        BigDecimal price = new BigDecimal("50000");

        when(queueTokenRepository.findByTokenUuid(invalidTokenUuid)).thenReturn(Optional.empty());

        ReservationUseCase.ReserveSeatCommand command = new ReservationUseCase.ReserveSeatCommand(
                invalidTokenUuid, userId, seatId, price
        );

        // When & Then
        assertThatThrownBy(() -> reservationUseCase.reserveSeat(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid token");

        verify(seatRepository, never()).findById(any());
        verify(seatRepository, never()).save(any());
        verify(reservationRepository, never()).save(any());
    }

    @Test
    @DisplayName("좌석 예약 실패 - 비활성화된 토큰")
    void reserveSeat_InactiveToken_ShouldThrowException() {
        // Given
        String tokenUuid = "inactive-token";
        Long userId = 1L;
        Long seatId = 1L;
        BigDecimal price = new BigDecimal("50000");

        QueueToken inactiveToken = new QueueToken(userId, 1L); // 활성화되지 않은 토큰

        when(queueTokenRepository.findByTokenUuid(tokenUuid)).thenReturn(Optional.of(inactiveToken));

        ReservationUseCase.ReserveSeatCommand command = new ReservationUseCase.ReserveSeatCommand(
                tokenUuid, userId, seatId, price
        );

        // When & Then
        assertThatThrownBy(() -> reservationUseCase.reserveSeat(command))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Token is not active");
    }

    @Test
    @DisplayName("좌석 예약 실패 - 존재하지 않는 좌석")
    void reserveSeat_SeatNotFound_ShouldThrowException() {
        // Given
        String tokenUuid = "test-token-uuid";
        Long userId = 1L;
        Long seatId = 999L;
        BigDecimal price = new BigDecimal("50000");

        QueueToken activeToken = new QueueToken(userId, 1L);
        activeToken.activate(10);

        when(queueTokenRepository.findByTokenUuid(tokenUuid)).thenReturn(Optional.of(activeToken));
        when(seatRepository.findById(seatId)).thenReturn(Optional.empty());

        ReservationUseCase.ReserveSeatCommand command = new ReservationUseCase.ReserveSeatCommand(
                tokenUuid, userId, seatId, price
        );

        // When & Then
        assertThatThrownBy(() -> reservationUseCase.reserveSeat(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Seat not found");
    }

    @Test
    @DisplayName("결제 처리 성공 - 임시 예약된 좌석의 결제 완료")
    void processPayment_Success() {
        // Given
        String tokenUuid = "test-token-uuid";
        Long userId = 1L;
        Long reservationId = 1L;
        BigDecimal price = new BigDecimal("50000");

        QueueToken activeToken = new QueueToken(userId, 1L);
        activeToken.activate(10);

        Concert concert = new Concert("Test Concert", "Test Artist", "Test Venue");
        ConcertSchedule schedule = new ConcertSchedule(concert, LocalDateTime.now().plusDays(1), LocalDateTime.now());
        Seat seat = new Seat(schedule, 1);
        seat.reserve(userId, 5); // 임시 예약

        Reservation reservation = new Reservation(userId, seat, price);
        PaymentInfo paymentInfo = new PaymentInfo("payment-123", price);

        when(queueTokenRepository.findByTokenUuid(tokenUuid)).thenReturn(Optional.of(activeToken));
        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));
        when(paymentService.processPayment(eq(userId), eq(price), anyString())).thenReturn(paymentInfo);

        ReservationUseCase.ProcessPaymentCommand command = new ReservationUseCase.ProcessPaymentCommand(
                tokenUuid, userId, reservationId
        );

        // When
        ReservationUseCase.PaymentResult result = reservationUseCase.processPayment(command);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getPaymentId()).isEqualTo("payment-123");
        assertThat(result.getReservationId()).isEqualTo(reservationId);
        assertThat(result.getAmount()).isEqualTo(price);
        assertThat(result.getPaidAt()).isNotNull();

        verify(userBalanceService).deductBalance(userId, price);
        verify(paymentService).processPayment(eq(userId), eq(price), anyString());
        verify(reservationRepository).save(reservation);
        verify(seatRepository).save(seat);
        verify(queueTokenRepository).save(activeToken);
    }

    @Test
    @DisplayName("결제 처리 실패 - 예약이 존재하지 않음")
    void processPayment_ReservationNotFound_ShouldThrowException() {
        // Given
        String tokenUuid = "test-token-uuid";
        Long userId = 1L;
        Long reservationId = 999L;

        QueueToken activeToken = new QueueToken(userId, 1L);
        activeToken.activate(10);

        when(queueTokenRepository.findByTokenUuid(tokenUuid)).thenReturn(Optional.of(activeToken));
        when(reservationRepository.findById(reservationId)).thenReturn(Optional.empty());

        ReservationUseCase.ProcessPaymentCommand command = new ReservationUseCase.ProcessPaymentCommand(
                tokenUuid, userId, reservationId
        );

        // When & Then
        assertThatThrownBy(() -> reservationUseCase.processPayment(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Reservation not found");

        verify(userBalanceService, never()).deductBalance(any(), any());
        verify(paymentService, never()).processPayment(any(), any(), any());
    }

    @Test
    @DisplayName("결제 처리 실패 - 다른 사용자의 예약")
    void processPayment_WrongUser_ShouldThrowException() {
        // Given
        String tokenUuid = "test-token-uuid";
        Long userId = 1L;
        Long otherUserId = 2L;
        Long reservationId = 1L;
        BigDecimal price = new BigDecimal("50000");

        QueueToken activeToken = new QueueToken(userId, 1L);
        activeToken.activate(10);

        Concert concert = new Concert("Test Concert", "Test Artist", "Test Venue");
        ConcertSchedule schedule = new ConcertSchedule(concert, LocalDateTime.now().plusDays(1), LocalDateTime.now());
        Seat seat = new Seat(schedule, 1);

        Reservation otherUserReservation = new Reservation(otherUserId, seat, price); // 다른 사용자의 예약

        when(queueTokenRepository.findByTokenUuid(tokenUuid)).thenReturn(Optional.of(activeToken));
        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(otherUserReservation));

        ReservationUseCase.ProcessPaymentCommand command = new ReservationUseCase.ProcessPaymentCommand(
                tokenUuid, userId, reservationId
        );

        // When & Then
        assertThatThrownBy(() -> reservationUseCase.processPayment(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Reservation does not belong to user");

        verify(userBalanceService, never()).deductBalance(any(), any());
        verify(paymentService, never()).processPayment(any(), any(), any());
    }
}