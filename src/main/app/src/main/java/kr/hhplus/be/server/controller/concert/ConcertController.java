package kr.hhplus.be.server.controller.concert;

import kr.hhplus.be.server.application.concert.ReservationUseCase;
import kr.hhplus.be.server.application.queue.QueueManagementUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/concerts")
public class ConcertController {

    private final ReservationUseCase reservationUseCase;
    private final QueueManagementUseCase queueManagementUseCase;

    public ConcertController(ReservationUseCase reservationUseCase,
                           QueueManagementUseCase queueManagementUseCase) {
        this.reservationUseCase = reservationUseCase;
        this.queueManagementUseCase = queueManagementUseCase;
    }

    @PostMapping("/queue/token")
    public ResponseEntity<QueueManagementUseCase.QueueTokenResult> issueQueueToken(
            @RequestBody IssueTokenRequest request) {
        QueueManagementUseCase.QueueTokenResult result = queueManagementUseCase.issueToken(request.getUserId());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/queue/status")
    public ResponseEntity<QueueManagementUseCase.QueueStatusResult> getQueueStatus(
            @RequestParam String tokenUuid) {
        QueueManagementUseCase.QueueStatusResult result = queueManagementUseCase.getQueueStatus(tokenUuid);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/schedules")
    public ResponseEntity<List<ReservationUseCase.AvailableScheduleInfo>> getAvailableSchedules(
            @RequestHeader("Queue-Token") String tokenUuid) {
        // 토큰 유효성 검증
        queueManagementUseCase.getQueueStatus(tokenUuid);

        List<ReservationUseCase.AvailableScheduleInfo> schedules = reservationUseCase.getAvailableSchedules();
        return ResponseEntity.ok(schedules);
    }

    @GetMapping("/schedules/{scheduleId}/seats")
    public ResponseEntity<List<ReservationUseCase.AvailableSeatInfo>> getAvailableSeats(
            @PathVariable Long scheduleId,
            @RequestHeader("Queue-Token") String tokenUuid) {
        // 토큰 유효성 검증
        queueManagementUseCase.getQueueStatus(tokenUuid);

        List<ReservationUseCase.AvailableSeatInfo> seats = reservationUseCase.getAvailableSeats(scheduleId);
        return ResponseEntity.ok(seats);
    }

    @PostMapping("/reservations")
    public ResponseEntity<ReservationUseCase.ReservationResult> reserveSeat(
            @RequestHeader("Queue-Token") String tokenUuid,
            @RequestBody ReserveSeatRequest request) {

        ReservationUseCase.ReserveSeatCommand command = new ReservationUseCase.ReserveSeatCommand(
                tokenUuid,
                request.getUserId(),
                request.getSeatId(),
                request.getPrice()
        );

        ReservationUseCase.ReservationResult result = reservationUseCase.reserveSeat(command);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/payments")
    public ResponseEntity<ReservationUseCase.PaymentResult> processPayment(
            @RequestHeader("Queue-Token") String tokenUuid,
            @RequestBody ProcessPaymentRequest request) {

        ReservationUseCase.ProcessPaymentCommand command = new ReservationUseCase.ProcessPaymentCommand(
                tokenUuid,
                request.getUserId(),
                request.getReservationId()
        );

        ReservationUseCase.PaymentResult result = reservationUseCase.processPayment(command);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/queue/statistics")
    public ResponseEntity<QueueManagementUseCase.QueueStatistics> getQueueStatistics() {
        QueueManagementUseCase.QueueStatistics statistics = queueManagementUseCase.getQueueStatistics();
        return ResponseEntity.ok(statistics);
    }

    // Request DTOs
    public static class IssueTokenRequest {
        private Long userId;

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
    }

    public static class ReserveSeatRequest {
        private Long userId;
        private Long seatId;
        private BigDecimal price;

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }

        public Long getSeatId() { return seatId; }
        public void setSeatId(Long seatId) { this.seatId = seatId; }

        public BigDecimal getPrice() { return price; }
        public void setPrice(BigDecimal price) { this.price = price; }
    }

    public static class ProcessPaymentRequest {
        private Long userId;
        private Long reservationId;

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }

        public Long getReservationId() { return reservationId; }
        public void setReservationId(Long reservationId) { this.reservationId = reservationId; }
    }
}