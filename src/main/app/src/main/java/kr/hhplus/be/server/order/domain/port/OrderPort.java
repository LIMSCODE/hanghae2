package kr.hhplus.be.server.order.domain.port;

import kr.hhplus.be.server.domain.Order;
import kr.hhplus.be.server.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderPort {
    Order saveOrder(Order order);
    Order findOrderById(Long orderId);
    Page<Order> findOrdersByUserId(Long userId, Pageable pageable);
}