package kr.hhplus.be.server.dto;

import kr.hhplus.be.server.domain.Coupon;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CouponResponse {

    private Long couponId;
    private String name;
    private BigDecimal discountAmount;
    private Integer totalQuantity;
    private Integer issuedQuantity;
    private Integer availableQuantity;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Boolean isActive;
    private Boolean canIssue;

    public CouponResponse(Coupon coupon) {
        this.couponId = coupon.getCouponId();
        this.name = coupon.getName();
        this.discountAmount = coupon.getDiscountAmount();
        this.totalQuantity = coupon.getTotalQuantity();
        this.issuedQuantity = coupon.getIssuedQuantity();
        this.availableQuantity = coupon.getAvailableQuantity();
        this.startDate = coupon.getStartDate();
        this.endDate = coupon.getEndDate();
        this.isActive = coupon.getIsActive();
        this.canIssue = coupon.canIssue();
    }

    // Getters
    public Long getCouponId() { return couponId; }
    public String getName() { return name; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public Integer getTotalQuantity() { return totalQuantity; }
    public Integer getIssuedQuantity() { return issuedQuantity; }
    public Integer getAvailableQuantity() { return availableQuantity; }
    public LocalDateTime getStartDate() { return startDate; }
    public LocalDateTime getEndDate() { return endDate; }
    public Boolean getIsActive() { return isActive; }
    public Boolean getCanIssue() { return canIssue; }
}