package kr.hhplus.be.server.infrastructure.concert;

import kr.hhplus.be.server.application.concert.ReservationUseCase.AvailableScheduleInfo;
import kr.hhplus.be.server.application.concert.ReservationUseCase.AvailableSeatInfo;
import kr.hhplus.be.server.domain.concert.Seat;
import kr.hhplus.be.server.domain.concert.repository.SeatRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JpaSeatRepository extends JpaRepository<Seat, Long>, SeatRepository {

    @Override
    default Seat save(Seat seat) {
        return saveAndFlush(seat);
    }

    @Query("SELECT s FROM Seat s WHERE s.schedule.scheduleId = :scheduleId")
    List<Seat> findByScheduleId(@Param("scheduleId") Long scheduleId);

    @Query("SELECT s FROM Seat s WHERE s.schedule.scheduleId = :scheduleId AND s.seatStatus = 'AVAILABLE'")
    List<Seat> findAvailableSeatsByScheduleId(@Param("scheduleId") Long scheduleId);

    @Query("SELECT new kr.hhplus.be.server.application.concert.ReservationUseCase$AvailableScheduleInfo(" +
           "cs.scheduleId, c.title, cs.concertDate, cs.availableSeats) " +
           "FROM ConcertSchedule cs " +
           "JOIN cs.concert c " +
           "WHERE cs.availableSeats > 0 " +
           "AND cs.reservationOpenAt <= CURRENT_TIMESTAMP " +
           "ORDER BY cs.concertDate")
    List<AvailableScheduleInfo> findAvailableSchedules();

    @Query("SELECT new kr.hhplus.be.server.application.concert.ReservationUseCase$AvailableSeatInfo(" +
           "s.seatId, s.seatNumber, CAST(50000 as java.math.BigDecimal)) " +
           "FROM Seat s " +
           "WHERE s.schedule.scheduleId = :scheduleId " +
           "AND s.seatStatus = 'AVAILABLE' " +
           "ORDER BY s.seatNumber")
    List<AvailableSeatInfo> findAvailableSeatsByScheduleIdWithPrice(@Param("scheduleId") Long scheduleId);
}