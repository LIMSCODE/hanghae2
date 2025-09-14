package kr.hhplus.be.server.order.infrastructure.adapter;

import kr.hhplus.be.server.domain.Product;
import kr.hhplus.be.server.order.domain.port.ProductPort;
import kr.hhplus.be.server.service.ProductService;
import org.springframework.stereotype.Component;

@Component
public class ProductAdapter implements ProductPort {

    private final ProductService productService;

    public ProductAdapter(ProductService productService) {
        this.productService = productService;
    }

    @Override
    public Product getProductWithLock(Long productId) {
        return productService.getProductWithLock(productId);
    }

    @Override
    public void validateStock(Product product, Integer quantity) {
        productService.validateStock(product, quantity);
    }

    @Override
    public void deductStock(Product product, Integer quantity) {
        productService.deductStock(product, quantity);
    }
}