package kr.hhplus.be.server.repository;

import kr.hhplus.be.server.domain.UserCoupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserCouponRepository extends JpaRepository<UserCoupon, Long> {

    boolean existsByUserIdAndCouponId(Long userId, Long couponId);

    List<UserCoupon> findByUserIdAndIsUsedFalse(Long userId);

    @Query("SELECT uc FROM UserCoupon uc WHERE uc.userId = :userId AND uc.couponId = :couponId")
    Optional<UserCoupon> findByUserIdAndCouponId(@Param("userId") Long userId,
                                                @Param("couponId") Long couponId);
}