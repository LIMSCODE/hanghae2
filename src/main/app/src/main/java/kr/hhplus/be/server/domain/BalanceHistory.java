package kr.hhplus.be.server.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "balance_histories")
public class BalanceHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "balance_history_id")
    private Long balanceHistoryId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 20)
    private TransactionType transactionType;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "balance_before", nullable = false, precision = 15, scale = 2)
    private BigDecimal balanceBefore;

    @Column(name = "balance_after", nullable = false, precision = 15, scale = 2)
    private BigDecimal balanceAfter;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected BalanceHistory() {
    }

    public BalanceHistory(Long userId, TransactionType transactionType, BigDecimal amount, 
                         BigDecimal balanceBefore, BigDecimal balanceAfter, String description) {
        this.userId = userId;
        this.transactionType = transactionType;
        this.amount = amount;
        this.balanceBefore = balanceBefore;
        this.balanceAfter = balanceAfter;
        this.description = description;
        this.createdAt = LocalDateTime.now();
    }

    public static BalanceHistory createChargeHistory(Long userId, BigDecimal amount, 
                                                   BigDecimal balanceBefore, BigDecimal balanceAfter) {
        return new BalanceHistory(userId, TransactionType.CHARGE, amount, balanceBefore, balanceAfter, 
                                "잔액 충전");
    }

    public static BalanceHistory createPaymentHistory(Long userId, BigDecimal amount, 
                                                    BigDecimal balanceBefore, BigDecimal balanceAfter, 
                                                    Long orderId) {
        return new BalanceHistory(userId, TransactionType.PAYMENT, amount, balanceBefore, balanceAfter, 
                                "주문 결제 - 주문번호: " + orderId);
    }

    public static BalanceHistory createRefundHistory(Long userId, BigDecimal amount, 
                                                   BigDecimal balanceBefore, BigDecimal balanceAfter, 
                                                   Long orderId) {
        return new BalanceHistory(userId, TransactionType.REFUND, amount, balanceBefore, balanceAfter, 
                                "주문 환불 - 주문번호: " + orderId);
    }

    public Long getBalanceHistoryId() {
        return balanceHistoryId;
    }

    public Long getUserId() {
        return userId;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getBalanceBefore() {
        return balanceBefore;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public enum TransactionType {
        CHARGE, PAYMENT, REFUND
    }
}