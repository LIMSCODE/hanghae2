package kr.hhplus.be.server.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

public class OrderRequest {

    @NotNull(message = "사용자 ID는 필수입니다.")
    private Long userId;

    @NotEmpty(message = "주문 상품은 최소 1개 이상이어야 합니다.")
    @Valid
    private List<OrderItemRequest> orderItems;

    public OrderRequest() {
    }

    public OrderRequest(Long userId, List<OrderItemRequest> orderItems) {
        this.userId = userId;
        this.orderItems = orderItems;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public List<OrderItemRequest> getOrderItems() {
        return orderItems;
    }

    public void setOrderItems(List<OrderItemRequest> orderItems) {
        this.orderItems = orderItems;
    }

    public static class OrderItemRequest {

        @NotNull(message = "상품 ID는 필수입니다.")
        private Long productId;

        @NotNull(message = "수량은 필수입니다.")
        @Positive(message = "수량은 1개 이상이어야 합니다.")
        private Integer quantity;

        public OrderItemRequest() {
        }

        public OrderItemRequest(Long productId, Integer quantity) {
            this.productId = productId;
            this.quantity = quantity;
        }

        public Long getProductId() {
            return productId;
        }

        public void setProductId(Long productId) {
            this.productId = productId;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }
    }
}