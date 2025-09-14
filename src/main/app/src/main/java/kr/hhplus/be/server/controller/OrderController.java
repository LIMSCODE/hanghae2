package kr.hhplus.be.server.controller;

import kr.hhplus.be.server.dto.ApiResponse;
import kr.hhplus.be.server.dto.OrderRequest;
import kr.hhplus.be.server.dto.OrderResponse;
import kr.hhplus.be.server.order.application.OrderUseCase;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
@Validated
public class OrderController {

    private final OrderUseCase orderUseCase;

    public OrderController(OrderUseCase orderUseCase) {
        this.orderUseCase = orderUseCase;
    }

    @PostMapping("/orders")
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @Valid @RequestBody OrderRequest request) {
        
        OrderResponse response = orderUseCase.createOrder(request);
        
        return ResponseEntity.ok(
            ApiResponse.success("주문이 완료되었습니다", response)
        );
    }

    @GetMapping("/orders/{orderId}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(@PathVariable Long orderId) {
        
        OrderResponse response = orderUseCase.getOrder(orderId);
        
        return ResponseEntity.ok(
            ApiResponse.success("주문 조회가 완료되었습니다", response)
        );
    }

    @GetMapping("/users/{userId}/orders")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getUserOrders(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Page<OrderResponse> response = orderUseCase.getUserOrders(userId, page, size);
        
        return ResponseEntity.ok(
            ApiResponse.success("주문 목록 조회가 완료되었습니다", response)
        );
    }
}