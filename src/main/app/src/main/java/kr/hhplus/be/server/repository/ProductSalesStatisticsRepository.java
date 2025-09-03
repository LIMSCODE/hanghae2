package kr.hhplus.be.server.repository;

import kr.hhplus.be.server.domain.ProductSalesStatistics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductSalesStatisticsRepository extends JpaRepository<ProductSalesStatistics, Long> {

    Optional<ProductSalesStatistics> findByProductIdAndSalesDate(Long productId, LocalDate salesDate);

    @Query("""
        SELECT new kr.hhplus.be.server.dto.PopularProductDto(
            p.productId, 
            p.name, 
            p.price, 
            SUM(pss.totalQuantity), 
            SUM(pss.totalAmount)
        )
        FROM ProductSalesStatistics pss 
        JOIN Product p ON pss.productId = p.productId
        WHERE pss.salesDate BETWEEN :startDate AND :endDate
        AND p.isActive = true
        GROUP BY p.productId, p.name, p.price
        ORDER BY SUM(pss.totalQuantity) DESC
    """)
    List<PopularProductDto> findTopProductsByPeriod(@Param("startDate") LocalDate startDate, 
                                                   @Param("endDate") LocalDate endDate);

    @Modifying
    @Query("""
        INSERT INTO ProductSalesStatistics (productId, salesDate, totalQuantity, totalAmount, createdAt, updatedAt)
        VALUES (:productId, :salesDate, :quantity, :amount, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        ON DUPLICATE KEY UPDATE 
        totalQuantity = totalQuantity + VALUES(totalQuantity),
        totalAmount = totalAmount + VALUES(totalAmount),
        updatedAt = CURRENT_TIMESTAMP
    """)
    void upsertDailySales(@Param("productId") Long productId, 
                         @Param("salesDate") LocalDate salesDate,
                         @Param("quantity") Integer quantity, 
                         @Param("amount") BigDecimal amount);

    interface PopularProductDto {
        Long getProductId();
        String getName();
        BigDecimal getPrice();
        Long getTotalQuantity();
        BigDecimal getTotalAmount();
    }
}