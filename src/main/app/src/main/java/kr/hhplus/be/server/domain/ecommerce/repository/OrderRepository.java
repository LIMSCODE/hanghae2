package kr.hhplus.be.server.domain.ecommerce.repository;

import kr.hhplus.be.server.domain.Order;
import kr.hhplus.be.server.domain.OrderItem;
import java.util.List;
import java.util.Optional;

public interface OrderRepository {
    Order save(Order order);
    Optional<Order> findById(Long orderId);
    Optional<Order> findByIdempotencyKey(String idempotencyKey);
    List<Order> findByUserId(Long userId);

    OrderItem save(OrderItem orderItem);
    List<OrderItem> findByOrderId(Long orderId);

    // 주문 상태별 조회
    List<Order> findByOrderStatus(Order.OrderStatus status);
    List<Order> findCompletedOrdersInLastDays(int days);
}