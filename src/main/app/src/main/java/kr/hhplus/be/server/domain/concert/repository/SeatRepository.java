package kr.hhplus.be.server.domain.concert.repository;

import kr.hhplus.be.server.domain.concert.Seat;
import kr.hhplus.be.server.application.concert.ReservationUseCase.AvailableScheduleInfo;
import kr.hhplus.be.server.application.concert.ReservationUseCase.AvailableSeatInfo;

import java.util.List;
import java.util.Optional;

public interface SeatRepository {
    Seat save(Seat seat);
    Optional<Seat> findById(Long seatId);
    List<Seat> findByScheduleId(Long scheduleId);
    List<Seat> findAvailableSeatsByScheduleId(Long scheduleId);
    List<AvailableScheduleInfo> findAvailableSchedules();
    List<AvailableSeatInfo> findAvailableSeatsByScheduleIdWithPrice(Long scheduleId);
}