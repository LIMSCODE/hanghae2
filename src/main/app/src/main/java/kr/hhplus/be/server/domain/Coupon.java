package kr.hhplus.be.server.domain;

import jakarta.persistence.*;
import kr.hhplus.be.server.exception.BusinessException;
import kr.hhplus.be.server.exception.ErrorCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "coupons")
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long couponId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private BigDecimal discountAmount;

    @Column(nullable = false)
    private Integer totalQuantity;

    @Column(nullable = false)
    private Integer issuedQuantity;

    @Column(nullable = false)
    private LocalDateTime startDate;

    @Column(nullable = false)
    private LocalDateTime endDate;

    @Column(nullable = false)
    private Boolean isActive;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    protected Coupon() {}

    public Coupon(String name, BigDecimal discountAmount, Integer totalQuantity,
                  LocalDateTime startDate, LocalDateTime endDate) {
        this.name = name;
        this.discountAmount = discountAmount;
        this.totalQuantity = totalQuantity;
        this.issuedQuantity = 0;
        this.startDate = startDate;
        this.endDate = endDate;
        this.isActive = true;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void issueCoupon() {
        if (!isActive) {
            throw new BusinessException(ErrorCode.COUPON_NOT_ACTIVE, "비활성화된 쿠폰입니다");
        }

        if (LocalDateTime.now().isBefore(startDate)) {
            throw new BusinessException(ErrorCode.COUPON_NOT_STARTED, "쿠폰 발급 기간이 아닙니다");
        }

        if (LocalDateTime.now().isAfter(endDate)) {
            throw new BusinessException(ErrorCode.COUPON_EXPIRED, "쿠폰 발급 기간이 만료되었습니다");
        }

        if (issuedQuantity >= totalQuantity) {
            throw new BusinessException(ErrorCode.COUPON_SOLD_OUT, "쿠폰이 모두 소진되었습니다");
        }

        this.issuedQuantity++;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean canIssue() {
        return isActive &&
               LocalDateTime.now().isAfter(startDate) &&
               LocalDateTime.now().isBefore(endDate) &&
               issuedQuantity < totalQuantity;
    }

    public Integer getAvailableQuantity() {
        return totalQuantity - issuedQuantity;
    }

    // Getters
    public Long getCouponId() { return couponId; }
    public String getName() { return name; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public Integer getTotalQuantity() { return totalQuantity; }
    public Integer getIssuedQuantity() { return issuedQuantity; }
    public LocalDateTime getStartDate() { return startDate; }
    public LocalDateTime getEndDate() { return endDate; }
    public Boolean getIsActive() { return isActive; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    // Setters for JPA
    public void setCouponId(Long couponId) { this.couponId = couponId; }
    public void setName(String name) { this.name = name; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }
    public void setTotalQuantity(Integer totalQuantity) { this.totalQuantity = totalQuantity; }
    public void setIssuedQuantity(Integer issuedQuantity) { this.issuedQuantity = issuedQuantity; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }
    public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}