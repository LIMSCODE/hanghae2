package kr.hhplus.be.server.domain.ecommerce.repository;

import kr.hhplus.be.server.domain.Coupon;
import kr.hhplus.be.server.domain.UserCoupon;
import java.util.List;
import java.util.Optional;

public interface CouponRepository {
    Coupon save(Coupon coupon);
    Optional<Coupon> findById(Long couponId);
    Optional<Coupon> findByIdWithLock(Long couponId);
    List<Coupon> findAvailableCoupons();

    UserCoupon save(UserCoupon userCoupon);
    List<UserCoupon> findByUserId(Long userId);
    List<UserCoupon> findByUserIdAndUsed(Long userId, boolean used);
    Optional<UserCoupon> findByUserIdAndCouponId(Long userId, Long couponId);

    // 선착순 쿠폰 발급
    boolean issueCoupon(Long userId, Long couponId);
    int getRemainingCouponCount(Long couponId);
}