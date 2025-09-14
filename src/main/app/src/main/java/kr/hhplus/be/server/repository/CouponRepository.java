package kr.hhplus.be.server.repository;

import jakarta.persistence.LockModeType;
import kr.hhplus.be.server.domain.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Coupon c WHERE c.couponId = :couponId")
    Optional<Coupon> findByIdWithLock(@Param("couponId") Long couponId);

    @Query("SELECT c FROM Coupon c WHERE c.isActive = true " +
           "AND c.startDate <= :now AND c.endDate > :now " +
           "AND c.issuedQuantity < c.totalQuantity")
    List<Coupon> findAvailableCoupons(@Param("now") LocalDateTime now);

    List<Coupon> findByIsActiveTrue();
}