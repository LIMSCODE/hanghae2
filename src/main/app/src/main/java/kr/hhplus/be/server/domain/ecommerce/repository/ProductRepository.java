package kr.hhplus.be.server.domain.ecommerce.repository;

import kr.hhplus.be.server.domain.Product;
import kr.hhplus.be.server.domain.ProductSalesStatistics;
import java.util.List;
import java.util.Optional;

public interface ProductRepository {
    Product save(Product product);
    Optional<Product> findById(Long productId);
    Optional<Product> findByIdWithLock(Long productId);
    List<Product> findAll();
    List<Product> findAllWithStock();

    // 재고 관리
    void decreaseStock(Long productId, Integer quantity);
    void increaseStock(Long productId, Integer quantity);

    // 판매 통계
    ProductSalesStatistics save(ProductSalesStatistics statistics);
    List<ProductSalesStatistics> findTop5ByOrderBySalesCountDesc();
    Optional<ProductSalesStatistics> findByProductId(Long productId);

    void updateSalesCount(Long productId, Integer quantity);
}