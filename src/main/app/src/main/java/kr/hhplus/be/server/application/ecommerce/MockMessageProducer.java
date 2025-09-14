package kr.hhplus.be.server.application.ecommerce;

import kr.hhplus.be.server.domain.ecommerce.Outbox;
import kr.hhplus.be.server.domain.ecommerce.repository.OutboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Random;

@Service
public class MockMessageProducer {

    private static final Logger logger = LoggerFactory.getLogger(MockMessageProducer.class);
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final Random random = new Random();

    public MockMessageProducer(OutboxRepository outboxRepository, ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void publishOrderCompletedEvent(Long orderId, Long userId, String totalAmount) {
        try {
            OrderCompletedEvent event = new OrderCompletedEvent(orderId, userId, totalAmount);
            String payload = objectMapper.writeValueAsString(event);

            Outbox outbox = new Outbox(
                    "ORDER_" + orderId,
                    "ORDER_COMPLETED",
                    payload
            );

            outboxRepository.save(outbox);
            logger.info("Order completed event saved to outbox: orderId={}", orderId);
        } catch (Exception e) {
            logger.error("Failed to publish order completed event: orderId={}", orderId, e);
        }
    }

    @Scheduled(fixedRate = 10000) // 10초마다 실행
    @Transactional
    public void processOutboxEvents() {
        // 1. 대기 중인 이벤트 처리
        List<Outbox> pendingEvents = outboxRepository.findPendingEvents();
        for (Outbox outbox : pendingEvents) {
            processEvent(outbox);
        }

        // 2. 실패한 이벤트 재시도
        List<Outbox> failedEvents = outboxRepository.findFailedEventsForRetry();
        for (Outbox outbox : failedEvents) {
            if (outbox.canRetry()) {
                outbox.retry();
                processEvent(outbox);
            }
        }

        // 3. 처리된 이벤트 정리 (7일 이상 지난 것들)
        outboxRepository.deleteProcessedEvents();
    }

    private void processEvent(Outbox outbox) {
        try {
            // Mock 외부 시스템 호출 (성공률 90%)
            boolean success = simulateExternalSystemCall(outbox);

            if (success) {
                outbox.markAsProcessed();
                logger.info("Successfully processed outbox event: id={}, type={}",
                           outbox.getOutboxId(), outbox.getEventType());
            } else {
                outbox.markAsFailed("External system call failed");
                logger.warn("Failed to process outbox event: id={}, type={}, retryCount={}",
                           outbox.getOutboxId(), outbox.getEventType(), outbox.getRetryCount());
            }

            outboxRepository.save(outbox);
        } catch (Exception e) {
            outbox.markAsFailed("Exception occurred: " + e.getMessage());
            outboxRepository.save(outbox);
            logger.error("Exception while processing outbox event: id={}", outbox.getOutboxId(), e);
        }
    }

    private boolean simulateExternalSystemCall(Outbox outbox) {
        // Mock 외부 시스템 호출 (데이터 플랫폼 전송 시뮬레이션)
        try {
            Thread.sleep(100); // 네트워크 지연 시뮬레이션

            // 90% 성공률
            boolean success = random.nextDouble() < 0.9;

            if (success) {
                logger.info("Mock external system call successful for event: {}", outbox.getEventType());
            } else {
                logger.warn("Mock external system call failed for event: {}", outbox.getEventType());
            }

            return success;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    // 이벤트 클래스들
    public static class OrderCompletedEvent {
        private Long orderId;
        private Long userId;
        private String totalAmount;
        private String timestamp;

        public OrderCompletedEvent(Long orderId, Long userId, String totalAmount) {
            this.orderId = orderId;
            this.userId = userId;
            this.totalAmount = totalAmount;
            this.timestamp = java.time.LocalDateTime.now().toString();
        }

        // Getters and Setters
        public Long getOrderId() { return orderId; }
        public void setOrderId(Long orderId) { this.orderId = orderId; }

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }

        public String getTotalAmount() { return totalAmount; }
        public void setTotalAmount(String totalAmount) { this.totalAmount = totalAmount; }

        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    }
}