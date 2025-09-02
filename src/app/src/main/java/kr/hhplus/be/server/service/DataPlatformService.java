package kr.hhplus.be.server.service;

import kr.hhplus.be.server.domain.Order;
import kr.hhplus.be.server.domain.OrderItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DataPlatformService {

    private static final Logger logger = LoggerFactory.getLogger(DataPlatformService.class);

    @Async
    @EventListener
    public void handleOrderCompletedEvent(OrderService.OrderCompletedEvent event) {
        Order order = event.getOrder();
        
        try {
            // 외부 데이터 플랫폼으로 주문 데이터 전송 (Mock)
            sendOrderDataToPlatform(order);
            
            logger.info("Order data sent to external platform successfully. OrderId: {}", 
                       order.getOrderId());
        } catch (Exception e) {
            logger.error("Failed to send order data to external platform. OrderId: {}, Error: {}", 
                        order.getOrderId(), e.getMessage(), e);
            
            // 실제 환경에서는 재시도 로직이나 Dead Letter Queue 처리 필요
        }
    }

    public void sendOrderDataToPlatform(Order order) {
        // Mock 구현: 실제로는 HTTP 클라이언트를 사용하여 외부 API 호출
        
        Map<String, Object> orderData = createOrderDataPayload(order);
        
        // 시뮬레이션: 네트워크 지연
        simulateNetworkDelay();
        
        // 시뮬레이션: 간헐적 실패 (10% 확률)
        if (Math.random() < 0.1) {
            throw new RuntimeException("External platform is temporarily unavailable");
        }
        
        logger.info("Mock: Order data sent to external platform: {}", orderData);
    }

    private Map<String, Object> createOrderDataPayload(Order order) {
        Map<String, Object> orderData = new HashMap<>();
        orderData.put("orderId", order.getOrderId());
        orderData.put("userId", order.getUserId());
        orderData.put("orderStatus", order.getOrderStatus().name());
        orderData.put("totalAmount", order.getTotalAmount());
        orderData.put("orderedAt", order.getOrderedAt());
        
        List<Map<String, Object>> items = order.getOrderItems().stream()
            .map(this::createOrderItemPayload)
            .collect(Collectors.toList());
        orderData.put("orderItems", items);
        
        orderData.put("timestamp", LocalDateTime.now());
        
        return orderData;
    }

    private Map<String, Object> createOrderItemPayload(OrderItem item) {
        Map<String, Object> itemData = new HashMap<>();
        itemData.put("productId", item.getProductId());
        itemData.put("quantity", item.getQuantity());
        itemData.put("unitPrice", item.getUnitPrice());
        itemData.put("subtotal", item.getSubtotal());
        return itemData;
    }

    private void simulateNetworkDelay() {
        try {
            // 100-500ms 랜덤 지연
            Thread.sleep(100 + (long) (Math.random() * 400));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Thread interrupted during network simulation", e);
        }
    }
}