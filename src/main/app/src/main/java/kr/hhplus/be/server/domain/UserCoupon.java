package kr.hhplus.be.server.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_coupons",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "coupon_id"}))
public class UserCoupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userCouponId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long couponId;

    @Column(nullable = false)
    private Boolean isUsed;

    private LocalDateTime usedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime issuedAt;

    protected UserCoupon() {}

    public UserCoupon(Long userId, Long couponId) {
        this.userId = userId;
        this.couponId = couponId;
        this.isUsed = false;
        this.issuedAt = LocalDateTime.now();
    }

    public void useCoupon() {
        this.isUsed = true;
        this.usedAt = LocalDateTime.now();
    }

    // Getters
    public Long getUserCouponId() { return userCouponId; }
    public Long getUserId() { return userId; }
    public Long getCouponId() { return couponId; }
    public Boolean getIsUsed() { return isUsed; }
    public LocalDateTime getUsedAt() { return usedAt; }
    public LocalDateTime getIssuedAt() { return issuedAt; }

    // Setters for JPA
    public void setUserCouponId(Long userCouponId) { this.userCouponId = userCouponId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public void setCouponId(Long couponId) { this.couponId = couponId; }
    public void setIsUsed(Boolean isUsed) { this.isUsed = isUsed; }
    public void setUsedAt(LocalDateTime usedAt) { this.usedAt = usedAt; }
    public void setIssuedAt(LocalDateTime issuedAt) { this.issuedAt = issuedAt; }
}