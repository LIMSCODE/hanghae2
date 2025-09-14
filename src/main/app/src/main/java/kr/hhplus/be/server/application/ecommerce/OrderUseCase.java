package kr.hhplus.be.server.application.ecommerce;

import kr.hhplus.be.server.domain.Order;
import kr.hhplus.be.server.domain.OrderItem;
import kr.hhplus.be.server.domain.Product;
import kr.hhplus.be.server.domain.ecommerce.repository.OrderRepository;
import kr.hhplus.be.server.domain.ecommerce.repository.ProductRepository;
import kr.hhplus.be.server.domain.ecommerce.repository.UserBalanceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class OrderUseCase {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserBalanceRepository userBalanceRepository;
    private final MockMessageProducer messageProducer;

    public OrderUseCase(OrderRepository orderRepository,
                       ProductRepository productRepository,
                       UserBalanceRepository userBalanceRepository,
                       MockMessageProducer messageProducer) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.userBalanceRepository = userBalanceRepository;
        this.messageProducer = messageProducer;
    }

    @Transactional
    public OrderResult processOrder(OrderCommand command) {
        // 1. Idempotency 체크
        if (command.getIdempotencyKey() != null) {
            Optional<Order> existingOrder = orderRepository.findByIdempotencyKey(command.getIdempotencyKey());
            if (existingOrder.isPresent()) {
                Order order = existingOrder.get();
                return new OrderResult(
                        order.getOrderId(),
                        order.getUserId(),
                        order.getTotalAmount(),
                        order.getOrderStatus().name(),
                        order.getIdempotencyKey(),
                        true // 중복 요청임을 표시
                );
            }
        }

        // 2. 주문 생성
        Order order = new Order(command.getUserId(), BigDecimal.ZERO, command.getIdempotencyKey());

        // 3. 주문 상품들 처리
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (OrderItemRequest item : command.getOrderItems()) {
            // 상품 조회 및 재고 확인
            Product product = productRepository.findByIdWithLock(item.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("Product not found: " + item.getProductId()));

            if (product.getStock() < item.getQuantity()) {
                throw new IllegalArgumentException("Insufficient stock for product: " + product.getName());
            }

            // 재고 차감
            productRepository.decreaseStock(item.getProductId(), item.getQuantity());

            // 주문 아이템 생성
            OrderItem orderItem = new OrderItem(order, product, item.getQuantity(), product.getPrice());
            order.addOrderItem(orderItem);

            // 총액 계산
            BigDecimal itemTotal = product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            totalAmount = totalAmount.add(itemTotal);

            // 판매 통계 업데이트
            productRepository.updateSalesCount(item.getProductId(), item.getQuantity());
        }

        // 4. 총액 설정
        order = new Order(order.getUserId(), totalAmount, order.getIdempotencyKey());
        for (OrderItem item : order.getOrderItems()) {
            order.addOrderItem(item);
        }

        // 5. 잔액 확인 및 차감
        userBalanceRepository.deductBalance(command.getUserId(), totalAmount);

        // 6. 주문 완료 처리
        order.complete();
        Order savedOrder = orderRepository.save(order);

        // 7. 외부 시스템에 주문 완료 이벤트 전송
        messageProducer.publishOrderCompletedEvent(
                savedOrder.getOrderId(),
                savedOrder.getUserId(),
                savedOrder.getTotalAmount().toString()
        );

        return new OrderResult(
                savedOrder.getOrderId(),
                savedOrder.getUserId(),
                savedOrder.getTotalAmount(),
                savedOrder.getOrderStatus().name(),
                savedOrder.getIdempotencyKey(),
                false
        );
    }

    // Command 및 Result 클래스들
    public static class OrderCommand {
        private final Long userId;
        private final List<OrderItemRequest> orderItems;
        private final String idempotencyKey;

        public OrderCommand(Long userId, List<OrderItemRequest> orderItems, String idempotencyKey) {
            this.userId = userId;
            this.orderItems = orderItems;
            this.idempotencyKey = idempotencyKey;
        }

        public Long getUserId() { return userId; }
        public List<OrderItemRequest> getOrderItems() { return orderItems; }
        public String getIdempotencyKey() { return idempotencyKey; }
    }

    public static class OrderItemRequest {
        private final Long productId;
        private final Integer quantity;

        public OrderItemRequest(Long productId, Integer quantity) {
            this.productId = productId;
            this.quantity = quantity;
        }

        public Long getProductId() { return productId; }
        public Integer getQuantity() { return quantity; }
    }

    public static class OrderResult {
        private final Long orderId;
        private final Long userId;
        private final BigDecimal totalAmount;
        private final String orderStatus;
        private final String idempotencyKey;
        private final boolean isDuplicate;

        public OrderResult(Long orderId, Long userId, BigDecimal totalAmount,
                          String orderStatus, String idempotencyKey, boolean isDuplicate) {
            this.orderId = orderId;
            this.userId = userId;
            this.totalAmount = totalAmount;
            this.orderStatus = orderStatus;
            this.idempotencyKey = idempotencyKey;
            this.isDuplicate = isDuplicate;
        }

        public Long getOrderId() { return orderId; }
        public Long getUserId() { return userId; }
        public BigDecimal getTotalAmount() { return totalAmount; }
        public String getOrderStatus() { return orderStatus; }
        public String getIdempotencyKey() { return idempotencyKey; }
        public boolean isDuplicate() { return isDuplicate; }
    }
}