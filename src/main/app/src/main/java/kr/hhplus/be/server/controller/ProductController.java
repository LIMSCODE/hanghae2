package kr.hhplus.be.server.controller;

import kr.hhplus.be.server.dto.ApiResponse;
import kr.hhplus.be.server.dto.PopularProductsResponse;
import kr.hhplus.be.server.dto.ProductResponse;
import kr.hhplus.be.server.service.ProductService;
import kr.hhplus.be.server.service.StatisticsService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;
    private final StatisticsService statisticsService;

    public ProductController(ProductService productService, StatisticsService statisticsService) {
        this.productService = productService;
        this.statisticsService = statisticsService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> getProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort) {
        
        Page<ProductResponse> response = productService.getProducts(page, size, sort);
        
        return ResponseEntity.ok(
            ApiResponse.success("상품 목록 조회가 완료되었습니다", response)
        );
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProduct(@PathVariable Long productId) {
        
        ProductResponse response = productService.getProduct(productId);
        
        return ResponseEntity.ok(
            ApiResponse.success("상품 상세 조회가 완료되었습니다", response)
        );
    }

    @GetMapping("/popular")
    public ResponseEntity<ApiResponse<PopularProductsResponse>> getPopularProducts(
            @RequestParam(defaultValue = "3") Integer days,
            @RequestParam(defaultValue = "5") Integer limit) {
        
        PopularProductsResponse response = statisticsService.getPopularProducts(days, limit);
        
        return ResponseEntity.ok(
            ApiResponse.success("인기 상품 조회가 완료되었습니다", response)
        );
    }
}