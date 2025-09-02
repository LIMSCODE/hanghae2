package kr.hhplus.be.server.service;

import kr.hhplus.be.server.domain.Product;
import kr.hhplus.be.server.dto.ProductResponse;
import kr.hhplus.be.server.exception.BusinessException;
import kr.hhplus.be.server.exception.ErrorCode;
import kr.hhplus.be.server.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public Page<ProductResponse> getProducts(int page, int size, String sort) {
        Sort.Direction direction = Sort.Direction.ASC;
        String property = "productId";
        
        if (sort != null) {
            switch (sort.toLowerCase()) {
                case "name":
                    property = "name";
                    break;
                case "price":
                    property = "price";
                    break;
                case "stock":
                    property = "stockQuantity";
                    break;
                default:
                    property = "productId";
            }
        }
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, property));
        
        return productRepository.findAllByIsActiveTrue(pageable)
                .map(ProductResponse::new);
    }

    public ProductResponse getProduct(Long productId) {
        Product product = productRepository.findByProductIdAndIsActiveTrue(productId)
            .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        
        return new ProductResponse(product);
    }

    @Transactional
    public Product getProductWithLock(Long productId) {
        return productRepository.findByIdWithLock(productId)
            .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
    }

    public Product getProductForOrder(Long productId) {
        return productRepository.findByProductIdAndIsActiveTrue(productId)
            .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
    }

    @Transactional
    public void deductStock(Product product, Integer quantity) {
        if (!product.hasEnoughStock(quantity)) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK, 
                String.format("재고가 부족합니다. 상품: %s, 요청: %d개, 재고: %d개", 
                    product.getName(), quantity, product.getStockQuantity()));
        }
        
        product.deductStock(quantity);
        productRepository.save(product);
    }

    public void validateStock(Product product, Integer quantity) {
        if (!product.hasEnoughStock(quantity)) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK, 
                String.format("재고가 부족합니다. 상품: %s, 요청: %d개, 재고: %d개", 
                    product.getName(), quantity, product.getStockQuantity()));
        }
    }
}