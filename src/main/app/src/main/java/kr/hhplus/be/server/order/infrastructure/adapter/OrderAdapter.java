package kr.hhplus.be.server.order.infrastructure.adapter;

import kr.hhplus.be.server.domain.Order;
import kr.hhplus.be.server.exception.BusinessException;
import kr.hhplus.be.server.exception.ErrorCode;
import kr.hhplus.be.server.order.domain.port.OrderPort;
import kr.hhplus.be.server.repository.OrderRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
public class OrderAdapter implements OrderPort {

    private final OrderRepository orderRepository;

    public OrderAdapter(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public Order saveOrder(Order order) {
        return orderRepository.save(order);
    }

    @Override
    public Order findOrderById(Long orderId) {
        Order order = orderRepository.findByIdWithItems(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        return order;
    }

    @Override
    public Page<Order> findOrdersByUserId(Long userId, Pageable pageable) {
        return orderRepository.findByUserIdOrderByOrderedAtDesc(userId, pageable);
    }
}