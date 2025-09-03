package kr.hhplus.be.server.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class BalanceResponse {

    private Long userId;
    private BigDecimal balance;
    private BigDecimal chargedAmount;
    private LocalDateTime chargedAt;
    private LocalDateTime lastUpdatedAt;

    public BalanceResponse() {
    }

    public BalanceResponse(Long userId, BigDecimal balance, LocalDateTime lastUpdatedAt) {
        this.userId = userId;
        this.balance = balance;
        this.lastUpdatedAt = lastUpdatedAt;
    }

    public BalanceResponse(Long userId, BigDecimal balance, BigDecimal chargedAmount, LocalDateTime chargedAt) {
        this.userId = userId;
        this.balance = balance;
        this.chargedAmount = chargedAmount;
        this.chargedAt = chargedAt;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public BigDecimal getChargedAmount() {
        return chargedAmount;
    }

    public void setChargedAmount(BigDecimal chargedAmount) {
        this.chargedAmount = chargedAmount;
    }

    public LocalDateTime getChargedAt() {
        return chargedAt;
    }

    public void setChargedAt(LocalDateTime chargedAt) {
        this.chargedAt = chargedAt;
    }

    public LocalDateTime getLastUpdatedAt() {
        return lastUpdatedAt;
    }

    public void setLastUpdatedAt(LocalDateTime lastUpdatedAt) {
        this.lastUpdatedAt = lastUpdatedAt;
    }
}