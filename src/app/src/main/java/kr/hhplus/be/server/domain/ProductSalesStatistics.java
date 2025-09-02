package kr.hhplus.be.server.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "product_sales_statistics",
       uniqueConstraints = @UniqueConstraint(columnNames = {"product_id", "sales_date"}))
public class ProductSalesStatistics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stat_id")
    private Long statId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "sales_date", nullable = false)
    private LocalDate salesDate;

    @Column(name = "total_quantity", nullable = false)
    private Integer totalQuantity = 0;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected ProductSalesStatistics() {
    }

    public ProductSalesStatistics(Long productId, LocalDate salesDate, Integer quantity, BigDecimal amount) {
        this.productId = productId;
        this.salesDate = salesDate;
        this.totalQuantity = quantity;
        this.totalAmount = amount;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void addSales(Integer quantity, BigDecimal amount) {
        this.totalQuantity += quantity;
        this.totalAmount = this.totalAmount.add(amount);
    }

    public static ProductSalesStatistics create(Long productId, LocalDate salesDate, 
                                              Integer quantity, BigDecimal amount) {
        return new ProductSalesStatistics(productId, salesDate, quantity, amount);
    }

    public Long getStatId() {
        return statId;
    }

    public Long getProductId() {
        return productId;
    }

    public LocalDate getSalesDate() {
        return salesDate;
    }

    public Integer getTotalQuantity() {
        return totalQuantity;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}