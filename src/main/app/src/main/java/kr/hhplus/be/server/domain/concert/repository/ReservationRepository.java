package kr.hhplus.be.server.domain.concert.repository;

import kr.hhplus.be.server.domain.concert.Reservation;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository {
    Reservation save(Reservation reservation);
    Optional<Reservation> findById(Long reservationId);
    List<Reservation> findByUserId(Long userId);
    List<Reservation> findExpiredTemporaryReservations();
}