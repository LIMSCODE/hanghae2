package kr.hhplus.be.server.order.application;

import kr.hhplus.be.server.domain.Order;
import kr.hhplus.be.server.domain.OrderItem;
import kr.hhplus.be.server.domain.Product;
import kr.hhplus.be.server.domain.User;
import kr.hhplus.be.server.dto.OrderRequest;
import kr.hhplus.be.server.dto.OrderResponse;
import kr.hhplus.be.server.exception.BusinessException;
import kr.hhplus.be.server.exception.ErrorCode;
import kr.hhplus.be.server.order.domain.port.EventPort;
import kr.hhplus.be.server.order.domain.port.OrderPort;
import kr.hhplus.be.server.order.domain.port.ProductPort;
import kr.hhplus.be.server.order.domain.port.UserPort;
import kr.hhplus.be.server.service.OrderService;
import kr.hhplus.be.server.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class OrderUseCase {

    private static final Logger logger = LoggerFactory.getLogger(OrderUseCase.class);

    private final UserPort userPort;
    private final ProductPort productPort;
    private final OrderPort orderPort;
    private final EventPort eventPort;
    private final UserService userService; // 잔액 조회용

    public OrderUseCase(UserPort userPort, 
                       ProductPort productPort, 
                       OrderPort orderPort, 
                       EventPort eventPort,
                       UserService userService) {
        this.userPort = userPort;
        this.productPort = productPort;
        this.orderPort = orderPort;
        this.eventPort = eventPort;
        this.userService = userService;
    }

    @Transactional
    public OrderResponse createOrder(OrderRequest orderRequest) {
        Long userId = orderRequest.getUserId();
        
        // 1. 사용자 조회 (비관적 락)
        User user = userPort.getUserWithLock(userId);
        
        // 2. 상품 조회 및 재고 확인 (비관적 락)
        Map<Long, Product> productMap = new HashMap<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        
        for (OrderRequest.OrderItemRequest itemRequest : orderRequest.getOrderItems()) {
            Product product = productPort.getProductWithLock(itemRequest.getProductId());
            productPort.validateStock(product, itemRequest.getQuantity());
            
            productMap.put(product.getProductId(), product);
            totalAmount = totalAmount.add(product.calculateSubtotal(itemRequest.getQuantity()));
        }
        
        // 3. 잔액 확인
        if (!user.hasEnoughBalance(totalAmount)) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_BALANCE, 
                String.format("잔액이 부족합니다. 현재 잔액: %s원", user.getBalance()));
        }
        
        // 4. 주문 생성
        Order order = new Order(userId, totalAmount);
        
        // 5. 주문 항목 생성 및 재고 차감
        for (OrderRequest.OrderItemRequest itemRequest : orderRequest.getOrderItems()) {
            Product product = productMap.get(itemRequest.getProductId());
            
            OrderItem orderItem = OrderItem.create(product, itemRequest.getQuantity());
            order.addOrderItem(orderItem);
            
            productPort.deductStock(product, itemRequest.getQuantity());
        }
        
        // 6. 결제 처리
        userPort.processPayment(user, totalAmount, null); // orderId는 저장 후 설정
        
        // 7. 주문 완료 처리
        order.complete();
        Order savedOrder = orderPort.saveOrder(order);
        
        // 8. 이벤트 발행 (비동기 처리용)
        OrderService.OrderCompletedEvent event = new OrderService.OrderCompletedEvent(savedOrder);
        eventPort.publishOrderCompletedEvent(event);
        
        logger.info("Order created successfully. OrderId: {}, UserId: {}, TotalAmount: {}", 
                   savedOrder.getOrderId(), userId, totalAmount);
        
        return new OrderResponse(savedOrder, user.getBalance());
    }

    public OrderResponse getOrder(Long orderId) {
        Order order = orderPort.findOrderById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        
        User user = userService.getUser(order.getUserId());
        return new OrderResponse(order, user.getBalance());
    }

    public Page<OrderResponse> getUserOrders(Long userId, int page, int size) {
        // 사용자 존재 확인
        userService.getUser(userId);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Order> orders = orderPort.findOrdersByUserId(userId, pageable);
        
        User user = userService.getUser(userId);
        return orders.map(order -> new OrderResponse(order, user.getBalance()));
    }
}