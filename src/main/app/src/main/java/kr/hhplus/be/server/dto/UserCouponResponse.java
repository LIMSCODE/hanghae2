package kr.hhplus.be.server.dto;

import kr.hhplus.be.server.domain.UserCoupon;

import java.time.LocalDateTime;

public class UserCouponResponse {

    private Long userCouponId;
    private Long userId;
    private Long couponId;
    private Boolean isUsed;
    private LocalDateTime usedAt;
    private LocalDateTime issuedAt;

    public UserCouponResponse(UserCoupon userCoupon) {
        this.userCouponId = userCoupon.getUserCouponId();
        this.userId = userCoupon.getUserId();
        this.couponId = userCoupon.getCouponId();
        this.isUsed = userCoupon.getIsUsed();
        this.usedAt = userCoupon.getUsedAt();
        this.issuedAt = userCoupon.getIssuedAt();
    }

    // Getters
    public Long getUserCouponId() { return userCouponId; }
    public Long getUserId() { return userId; }
    public Long getCouponId() { return couponId; }
    public Boolean getIsUsed() { return isUsed; }
    public LocalDateTime getUsedAt() { return usedAt; }
    public LocalDateTime getIssuedAt() { return issuedAt; }
}