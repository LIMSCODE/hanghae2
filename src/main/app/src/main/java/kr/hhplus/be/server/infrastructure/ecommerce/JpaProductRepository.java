package kr.hhplus.be.server.infrastructure.ecommerce;

import kr.hhplus.be.server.domain.Product;
import kr.hhplus.be.server.domain.ProductSalesStatistics;
import kr.hhplus.be.server.domain.ecommerce.repository.ProductRepository;
import kr.hhplus.be.server.repository.ProductSalesStatisticsRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

interface JpaProductRepositoryInterface extends JpaRepository<Product, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.productId = :productId")
    Optional<Product> findByIdWithLock(@Param("productId") Long productId);

    @Query("SELECT p FROM Product p WHERE p.stock > 0")
    List<Product> findAllWithStock();

    @Modifying
    @Query("UPDATE Product p SET p.stock = p.stock - :quantity WHERE p.productId = :productId AND p.stock >= :quantity")
    int decreaseStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);

    @Modifying
    @Query("UPDATE Product p SET p.stock = p.stock + :quantity WHERE p.productId = :productId")
    int increaseStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);
}

@Repository
public class JpaProductRepository implements ProductRepository {

    private final JpaProductRepositoryInterface jpaRepository;
    private final ProductSalesStatisticsRepository statisticsRepository;

    public JpaProductRepository(JpaProductRepositoryInterface jpaRepository,
                               ProductSalesStatisticsRepository statisticsRepository) {
        this.jpaRepository = jpaRepository;
        this.statisticsRepository = statisticsRepository;
    }

    @Override
    public Product save(Product product) {
        return jpaRepository.save(product);
    }

    @Override
    public Optional<Product> findById(Long productId) {
        return jpaRepository.findById(productId);
    }

    @Override
    public Optional<Product> findByIdWithLock(Long productId) {
        return jpaRepository.findByIdWithLock(productId);
    }

    @Override
    public List<Product> findAll() {
        return jpaRepository.findAll();
    }

    @Override
    public List<Product> findAllWithStock() {
        return jpaRepository.findAllWithStock();
    }

    @Override
    @Transactional
    public void decreaseStock(Long productId, Integer quantity) {
        int updated = jpaRepository.decreaseStock(productId, quantity);
        if (updated == 0) {
            throw new IllegalArgumentException("Insufficient stock or product not found");
        }
    }

    @Override
    @Transactional
    public void increaseStock(Long productId, Integer quantity) {
        jpaRepository.increaseStock(productId, quantity);
    }

    @Override
    public ProductSalesStatistics save(ProductSalesStatistics statistics) {
        return statisticsRepository.save(statistics);
    }

    @Override
    public List<ProductSalesStatistics> findTop5ByOrderBySalesCountDesc() {
        return statisticsRepository.findTop5ByOrderBySalesCountDesc();
    }

    @Override
    public Optional<ProductSalesStatistics> findByProductId(Long productId) {
        return statisticsRepository.findByProductId(productId);
    }

    @Override
    @Transactional
    public void updateSalesCount(Long productId, Integer quantity) {
        Optional<ProductSalesStatistics> statistics = findByProductId(productId);

        if (statistics.isPresent()) {
            ProductSalesStatistics stat = statistics.get();
            stat.increaseSalesCount(quantity);
            save(stat);
        } else {
            // 새로운 판매 통계 생성
            Product product = findById(productId)
                    .orElseThrow(() -> new IllegalArgumentException("Product not found"));
            ProductSalesStatistics newStat = new ProductSalesStatistics(productId, product.getName(), quantity);
            save(newStat);
        }
    }
}