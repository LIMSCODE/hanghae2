package kr.hhplus.be.server.application.concert;

import kr.hhplus.be.server.domain.concert.Reservation;
import kr.hhplus.be.server.domain.concert.Seat;
import kr.hhplus.be.server.domain.queue.QueueToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReservationUseCase {

    private final SeatRepository seatRepository;
    private final ReservationRepository reservationRepository;
    private final QueueTokenRepository queueTokenRepository;
    private final UserBalanceService userBalanceService;
    private final PaymentService paymentService;

    public ReservationUseCase(SeatRepository seatRepository,
                            ReservationRepository reservationRepository,
                            QueueTokenRepository queueTokenRepository,
                            UserBalanceService userBalanceService,
                            PaymentService paymentService) {
        this.seatRepository = seatRepository;
        this.reservationRepository = reservationRepository;
        this.queueTokenRepository = queueTokenRepository;
        this.userBalanceService = userBalanceService;
        this.paymentService = paymentService;
    }

    @Transactional
    public ReservationResult reserveSeat(ReserveSeatCommand command) {
        // 1. 토큰 검증
        QueueToken token = queueTokenRepository.findByTokenUuid(command.getTokenUuid())
                .orElseThrow(() -> new IllegalArgumentException("Invalid token"));

        if (!token.isActive()) {
            throw new IllegalStateException("Token is not active");
        }

        // 2. 좌석 조회 및 예약 가능성 확인
        Seat seat = seatRepository.findById(command.getSeatId())
                .orElseThrow(() -> new IllegalArgumentException("Seat not found"));

        if (!seat.isAvailable()) {
            throw new IllegalStateException("Seat is not available");
        }

        // 3. 좌석 임시 예약 (5분)
        seat.reserve(command.getUserId(), 5);
        seatRepository.save(seat);

        // 4. 예약 정보 저장
        Reservation reservation = new Reservation(command.getUserId(), seat, command.getPrice());
        reservationRepository.save(reservation);

        // 5. 좌석 예약 현황 업데이트
        seat.getSchedule().decreaseAvailableSeats();

        return new ReservationResult(
                reservation.getReservationId(),
                seat.getSeatNumber(),
                reservation.getPrice(),
                reservation.getExpiresAt()
        );
    }

    @Transactional
    public PaymentResult processPayment(ProcessPaymentCommand command) {
        // 1. 토큰 검증
        QueueToken token = queueTokenRepository.findByTokenUuid(command.getTokenUuid())
                .orElseThrow(() -> new IllegalArgumentException("Invalid token"));

        if (!token.isActive()) {
            throw new IllegalStateException("Token is not active");
        }

        // 2. 예약 조회 및 검증
        Reservation reservation = reservationRepository.findById(command.getReservationId())
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found"));

        if (!reservation.getUserId().equals(command.getUserId())) {
            throw new IllegalArgumentException("Reservation does not belong to user");
        }

        if (!reservation.isTemporary()) {
            throw new IllegalStateException("Reservation is not in temporary state");
        }

        if (reservation.isExpired()) {
            throw new IllegalStateException("Reservation has expired");
        }

        // 3. 잔액 확인 및 차감
        userBalanceService.deductBalance(command.getUserId(), reservation.getPrice());

        // 4. 결제 처리
        PaymentInfo paymentInfo = paymentService.processPayment(
                command.getUserId(),
                reservation.getPrice(),
                "Concert Seat Reservation - " + reservation.getSeat().getSeatNumber()
        );

        // 5. 예약 확정
        reservation.confirm();
        reservation.getSeat().confirmReservation();

        // 6. 토큰 완료 처리
        token.complete();

        reservationRepository.save(reservation);
        seatRepository.save(reservation.getSeat());
        queueTokenRepository.save(token);

        return new PaymentResult(
                paymentInfo.getPaymentId(),
                reservation.getReservationId(),
                reservation.getPrice(),
                LocalDateTime.now()
        );
    }

    @Transactional(readOnly = true)
    public List<AvailableScheduleInfo> getAvailableSchedules() {
        return seatRepository.findAvailableSchedules();
    }

    @Transactional(readOnly = true)
    public List<AvailableSeatInfo> getAvailableSeats(Long scheduleId) {
        return seatRepository.findAvailableSeatsByScheduleId(scheduleId);
    }

    // Command 클래스들
    public static class ReserveSeatCommand {
        private final String tokenUuid;
        private final Long userId;
        private final Long seatId;
        private final BigDecimal price;

        public ReserveSeatCommand(String tokenUuid, Long userId, Long seatId, BigDecimal price) {
            this.tokenUuid = tokenUuid;
            this.userId = userId;
            this.seatId = seatId;
            this.price = price;
        }

        public String getTokenUuid() { return tokenUuid; }
        public Long getUserId() { return userId; }
        public Long getSeatId() { return seatId; }
        public BigDecimal getPrice() { return price; }
    }

    public static class ProcessPaymentCommand {
        private final String tokenUuid;
        private final Long userId;
        private final Long reservationId;

        public ProcessPaymentCommand(String tokenUuid, Long userId, Long reservationId) {
            this.tokenUuid = tokenUuid;
            this.userId = userId;
            this.reservationId = reservationId;
        }

        public String getTokenUuid() { return tokenUuid; }
        public Long getUserId() { return userId; }
        public Long getReservationId() { return reservationId; }
    }

    // Result 클래스들
    public static class ReservationResult {
        private final Long reservationId;
        private final Integer seatNumber;
        private final BigDecimal price;
        private final LocalDateTime expiresAt;

        public ReservationResult(Long reservationId, Integer seatNumber, BigDecimal price, LocalDateTime expiresAt) {
            this.reservationId = reservationId;
            this.seatNumber = seatNumber;
            this.price = price;
            this.expiresAt = expiresAt;
        }

        public Long getReservationId() { return reservationId; }
        public Integer getSeatNumber() { return seatNumber; }
        public BigDecimal getPrice() { return price; }
        public LocalDateTime getExpiresAt() { return expiresAt; }
    }

    public static class PaymentResult {
        private final String paymentId;
        private final Long reservationId;
        private final BigDecimal amount;
        private final LocalDateTime paidAt;

        public PaymentResult(String paymentId, Long reservationId, BigDecimal amount, LocalDateTime paidAt) {
            this.paymentId = paymentId;
            this.reservationId = reservationId;
            this.amount = amount;
            this.paidAt = paidAt;
        }

        public String getPaymentId() { return paymentId; }
        public Long getReservationId() { return reservationId; }
        public BigDecimal getAmount() { return amount; }
        public LocalDateTime getPaidAt() { return paidAt; }
    }

    public static class AvailableScheduleInfo {
        private final Long scheduleId;
        private final String concertTitle;
        private final LocalDateTime concertDate;
        private final Integer availableSeats;

        public AvailableScheduleInfo(Long scheduleId, String concertTitle, LocalDateTime concertDate, Integer availableSeats) {
            this.scheduleId = scheduleId;
            this.concertTitle = concertTitle;
            this.concertDate = concertDate;
            this.availableSeats = availableSeats;
        }

        public Long getScheduleId() { return scheduleId; }
        public String getConcertTitle() { return concertTitle; }
        public LocalDateTime getConcertDate() { return concertDate; }
        public Integer getAvailableSeats() { return availableSeats; }
    }

    public static class AvailableSeatInfo {
        private final Long seatId;
        private final Integer seatNumber;
        private final BigDecimal price;

        public AvailableSeatInfo(Long seatId, Integer seatNumber, BigDecimal price) {
            this.seatId = seatId;
            this.seatNumber = seatNumber;
            this.price = price;
        }

        public Long getSeatId() { return seatId; }
        public Integer getSeatNumber() { return seatNumber; }
        public BigDecimal getPrice() { return price; }
    }
}

// Repository 인터페이스들 (Domain Layer)
interface SeatRepository {
    Seat findById(Long seatId);
    void save(Seat seat);
    List<ReservationUseCase.AvailableScheduleInfo> findAvailableSchedules();
    List<ReservationUseCase.AvailableSeatInfo> findAvailableSeatsByScheduleId(Long scheduleId);
}

interface ReservationRepository {
    void save(Reservation reservation);
    Reservation findById(Long reservationId);
}

interface QueueTokenRepository {
    java.util.Optional<QueueToken> findByTokenUuid(String tokenUuid);
    void save(QueueToken token);
}

// Service 인터페이스들 (Domain Layer)
interface UserBalanceService {
    void deductBalance(Long userId, BigDecimal amount);
}

interface PaymentService {
    PaymentInfo processPayment(Long userId, BigDecimal amount, String description);
}

class PaymentInfo {
    private final String paymentId;
    private final BigDecimal amount;

    public PaymentInfo(String paymentId, BigDecimal amount) {
        this.paymentId = paymentId;
        this.amount = amount;
    }

    public String getPaymentId() { return paymentId; }
    public BigDecimal getAmount() { return amount; }
}