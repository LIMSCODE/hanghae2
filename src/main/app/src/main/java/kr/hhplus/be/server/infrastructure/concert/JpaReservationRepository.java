package kr.hhplus.be.server.infrastructure.concert;

import kr.hhplus.be.server.domain.concert.Reservation;
import kr.hhplus.be.server.domain.concert.repository.ReservationRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JpaReservationRepository extends JpaRepository<Reservation, Long>, ReservationRepository {

    @Override
    default Reservation save(Reservation reservation) {
        return saveAndFlush(reservation);
    }

    @Query("SELECT r FROM Reservation r WHERE r.userId = :userId ORDER BY r.createdAt DESC")
    List<Reservation> findByUserId(@Param("userId") Long userId);

    @Query("SELECT r FROM Reservation r WHERE r.reservationStatus = 'TEMPORARY' AND r.expiresAt <= CURRENT_TIMESTAMP")
    List<Reservation> findExpiredTemporaryReservations();
}