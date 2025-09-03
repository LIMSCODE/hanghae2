package kr.hhplus.be.server.service;

import kr.hhplus.be.server.domain.Order;
import kr.hhplus.be.server.domain.OrderItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class StatisticsUpdateService {

    private static final Logger logger = LoggerFactory.getLogger(StatisticsUpdateService.class);

    private final StatisticsService statisticsService;

    public StatisticsUpdateService(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @Async
    @EventListener
    public void handleOrderCompletedEvent(OrderService.OrderCompletedEvent event) {
        Order order = event.getOrder();
        
        try {
            updateSalesStatistics(order);
            
            logger.info("Sales statistics updated successfully for order: {}", order.getOrderId());
        } catch (Exception e) {
            logger.error("Failed to update sales statistics for order: {}, Error: {}", 
                        order.getOrderId(), e.getMessage(), e);
        }
    }

    private void updateSalesStatistics(Order order) {
        for (OrderItem orderItem : order.getOrderItems()) {
            statisticsService.updateSalesStatisticsWithUpsert(
                orderItem.getProductId(),
                orderItem.getQuantity(),
                orderItem.getSubtotal()
            );
        }
    }
}