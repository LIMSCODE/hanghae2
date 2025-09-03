package kr.hhplus.be.server.dto;

import kr.hhplus.be.server.domain.Order;
import kr.hhplus.be.server.domain.OrderItem;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class OrderResponse {

    private Long orderId;
    private Long userId;
    private String orderStatus;
    private BigDecimal totalAmount;
    private BigDecimal remainingBalance;
    private List<OrderItemResponse> orderItems;
    private LocalDateTime orderedAt;

    public OrderResponse() {
    }

    public OrderResponse(Order order, BigDecimal remainingBalance) {
        this.orderId = order.getOrderId();
        this.userId = order.getUserId();
        this.orderStatus = order.getOrderStatus().name();
        this.totalAmount = order.getTotalAmount();
        this.remainingBalance = remainingBalance;
        this.orderItems = order.getOrderItems().stream()
                .map(OrderItemResponse::new)
                .collect(Collectors.toList());
        this.orderedAt = order.getOrderedAt();
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(String orderStatus) {
        this.orderStatus = orderStatus;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getRemainingBalance() {
        return remainingBalance;
    }

    public void setRemainingBalance(BigDecimal remainingBalance) {
        this.remainingBalance = remainingBalance;
    }

    public List<OrderItemResponse> getOrderItems() {
        return orderItems;
    }

    public void setOrderItems(List<OrderItemResponse> orderItems) {
        this.orderItems = orderItems;
    }

    public LocalDateTime getOrderedAt() {
        return orderedAt;
    }

    public void setOrderedAt(LocalDateTime orderedAt) {
        this.orderedAt = orderedAt;
    }

    public static class OrderItemResponse {
        private Long productId;
        private String productName;
        private BigDecimal price;
        private Integer quantity;
        private BigDecimal subtotal;

        public OrderItemResponse() {
        }

        public OrderItemResponse(OrderItem orderItem) {
            this.productId = orderItem.getProductId();
            this.price = orderItem.getUnitPrice();
            this.quantity = orderItem.getQuantity();
            this.subtotal = orderItem.getSubtotal();
        }

        public Long getProductId() {
            return productId;
        }

        public void setProductId(Long productId) {
            this.productId = productId;
        }

        public String getProductName() {
            return productName;
        }

        public void setProductName(String productName) {
            this.productName = productName;
        }

        public BigDecimal getPrice() {
            return price;
        }

        public void setPrice(BigDecimal price) {
            this.price = price;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }

        public BigDecimal getSubtotal() {
            return subtotal;
        }

        public void setSubtotal(BigDecimal subtotal) {
            this.subtotal = subtotal;
        }
    }
}