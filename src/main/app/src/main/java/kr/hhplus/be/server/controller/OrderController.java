package kr.hhplus.be.server.controller;

import kr.hhplus.be.server.dto.ApiResponse;
import kr.hhplus.be.server.dto.OrderRequest;
import kr.hhplus.be.server.dto.OrderResponse;
import kr.hhplus.be.server.service.OrderService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
@Validated
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/orders")
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @Valid @RequestBody OrderRequest request) {
        
        OrderResponse response = orderService.createOrder(request);
        
        return ResponseEntity.ok(
            ApiResponse.success("주문이 완료되었습니다", response)
        );
    }

    @GetMapping("/orders/{orderId}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(@PathVariable Long orderId) {
        
        OrderResponse response = orderService.getOrder(orderId);
        
        return ResponseEntity.ok(
            ApiResponse.success("주문 조회가 완료되었습니다", response)
        );
    }

    @GetMapping("/users/{userId}/orders")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getUserOrders(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Page<OrderResponse> response = orderService.getUserOrders(userId, page, size);
        
        return ResponseEntity.ok(
            ApiResponse.success("주문 목록 조회가 완료되었습니다", response)
        );
    }
}