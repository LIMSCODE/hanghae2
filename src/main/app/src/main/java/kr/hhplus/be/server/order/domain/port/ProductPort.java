package kr.hhplus.be.server.order.domain.port;

import kr.hhplus.be.server.domain.Product;

public interface ProductPort {
    Product getProductWithLock(Long productId);
    void validateStock(Product product, Integer quantity);
    void deductStock(Product product, Integer quantity);
}