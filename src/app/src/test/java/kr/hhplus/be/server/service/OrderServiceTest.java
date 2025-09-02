package kr.hhplus.be.server.service;

import kr.hhplus.be.server.domain.Order;
import kr.hhplus.be.server.domain.Product;
import kr.hhplus.be.server.domain.User;
import kr.hhplus.be.server.dto.OrderRequest;
import kr.hhplus.be.server.dto.OrderResponse;
import kr.hhplus.be.server.exception.BusinessException;
import kr.hhplus.be.server.exception.ErrorCode;
import kr.hhplus.be.server.repository.OrderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doNothing;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService 테스트")
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserService userService;

    @Mock
    private ProductService productService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private OrderService orderService;

    @Nested
    @DisplayName("주문 생성 테스트")
    class CreateOrderTest {

        @Test
        @DisplayName("정상적인 주문 생성")
        void createOrder_Success() {
            // given
            Long userId = 1L;
            Long productId1 = 1L;
            Long productId2 = 2L;
            
            OrderRequest.OrderItemRequest item1 = new OrderRequest.OrderItemRequest(productId1, 2);
            OrderRequest.OrderItemRequest item2 = new OrderRequest.OrderItemRequest(productId2, 1);
            List<OrderRequest.OrderItemRequest> items = Arrays.asList(item1, item2);
            OrderRequest orderRequest = new OrderRequest(userId, items);

            User user = new User("testUser", "test@example.com");
            user.chargeBalance(BigDecimal.valueOf(100000));

            Product product1 = new Product("iPhone 15", "Apple iPhone", BigDecimal.valueOf(30000), 10);
            Product product2 = new Product("Galaxy S24", "Samsung Galaxy", BigDecimal.valueOf(25000), 5);

            Order savedOrder = new Order(userId, BigDecimal.valueOf(85000));
            
            given(userService.getUserWithLock(userId)).willReturn(user);
            given(productService.getProductWithLock(productId1)).willReturn(product1);
            given(productService.getProductWithLock(productId2)).willReturn(product2);
            doNothing().when(productService).validateStock(any(Product.class), any(Integer.class));
            doNothing().when(productService).deductStock(any(Product.class), any(Integer.class));
            doNothing().when(userService).processPayment(any(User.class), any(BigDecimal.class), any());
            given(orderRepository.save(any(Order.class))).willReturn(savedOrder);

            // when
            OrderResponse response = orderService.createOrder(orderRequest);

            // then
            assertThat(response.getUserId()).isEqualTo(userId);
            assertThat(response.getTotalAmount()).isEqualTo(BigDecimal.valueOf(85000));
            assertThat(response.getOrderStatus()).isEqualTo("COMPLETED");
            
            verify(userService).getUserWithLock(userId);
            verify(productService).validateStock(product1, 2);
            verify(productService).validateStock(product2, 1);
            verify(productService).deductStock(product1, 2);
            verify(productService).deductStock(product2, 1);
            verify(userService).processPayment(any(User.class), any(BigDecimal.class), any());
            verify(orderRepository).save(any(Order.class));
            verify(eventPublisher).publishEvent(any(OrderService.OrderCompletedEvent.class));
        }

        @Test
        @DisplayName("존재하지 않는 사용자 주문 시 예외 발생")
        void createOrder_UserNotFound_ThrowsException() {
            // given
            Long userId = 1L;
            OrderRequest.OrderItemRequest item = new OrderRequest.OrderItemRequest(1L, 1);
            OrderRequest orderRequest = new OrderRequest(userId, Arrays.asList(item));
            
            given(userService.getUserWithLock(userId))
                .willThrow(new BusinessException(ErrorCode.USER_NOT_FOUND));

            // when & then
            assertThatThrownBy(() -> orderService.createOrder(orderRequest))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> {
                    BusinessException be = (BusinessException) exception;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
                });
        }

        @Test
        @DisplayName("잔액 부족 시 예외 발생")
        void createOrder_InsufficientBalance_ThrowsException() {
            // given
            Long userId = 1L;
            Long productId = 1L;
            
            OrderRequest.OrderItemRequest item = new OrderRequest.OrderItemRequest(productId, 2);
            OrderRequest orderRequest = new OrderRequest(userId, Arrays.asList(item));

            User user = new User("testUser", "test@example.com");
            user.chargeBalance(BigDecimal.valueOf(10000));  // 부족한 잔액

            Product product = new Product("iPhone 15", "Apple iPhone", BigDecimal.valueOf(30000), 10);
            
            given(userService.getUserWithLock(userId)).willReturn(user);
            given(productService.getProductWithLock(productId)).willReturn(product);
            doNothing().when(productService).validateStock(any(Product.class), any(Integer.class));

            // when & then
            assertThatThrownBy(() -> orderService.createOrder(orderRequest))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> {
                    BusinessException be = (BusinessException) exception;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.INSUFFICIENT_BALANCE);
                });
        }

        @Test
        @DisplayName("재고 부족 시 예외 발생")
        void createOrder_InsufficientStock_ThrowsException() {
            // given
            Long userId = 1L;
            Long productId = 1L;
            
            OrderRequest.OrderItemRequest item = new OrderRequest.OrderItemRequest(productId, 5);
            OrderRequest orderRequest = new OrderRequest(userId, Arrays.asList(item));

            User user = new User("testUser", "test@example.com");
            user.chargeBalance(BigDecimal.valueOf(200000));

            Product product = new Product("iPhone 15", "Apple iPhone", BigDecimal.valueOf(30000), 10);
            
            given(userService.getUserWithLock(userId)).willReturn(user);
            given(productService.getProductWithLock(productId)).willReturn(product);
            given(productService).validateStock(product, 5);
            doThrow(new BusinessException(ErrorCode.INSUFFICIENT_STOCK))
                .when(productService).validateStock(product, 5);

            // when & then
            assertThatThrownBy(() -> orderService.createOrder(orderRequest))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> {
                    BusinessException be = (BusinessException) exception;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.INSUFFICIENT_STOCK);
                });
        }
    }

    @Nested
    @DisplayName("주문 조회 테스트")
    class GetOrderTest {

        @Test
        @DisplayName("정상적인 주문 조회")
        void getOrder_Success() {
            // given
            Long orderId = 1L;
            Long userId = 1L;
            
            Order order = new Order(userId, BigDecimal.valueOf(50000));
            User user = new User("testUser", "test@example.com");
            user.chargeBalance(BigDecimal.valueOf(100000));
            
            given(orderRepository.findByIdWithItems(orderId)).willReturn(order);
            given(userService.getUser(userId)).willReturn(user);

            // when
            OrderResponse response = orderService.getOrder(orderId);

            // then
            assertThat(response.getOrderId()).isEqualTo(orderId);
            assertThat(response.getUserId()).isEqualTo(userId);
            assertThat(response.getTotalAmount()).isEqualTo(BigDecimal.valueOf(50000));
        }

        @Test
        @DisplayName("존재하지 않는 주문 조회 시 예외 발생")
        void getOrder_NotFound_ThrowsException() {
            // given
            Long orderId = 1L;
            
            given(orderRepository.findByIdWithItems(orderId)).willReturn(null);

            // when & then
            assertThatThrownBy(() -> orderService.getOrder(orderId))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> {
                    BusinessException be = (BusinessException) exception;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.ORDER_NOT_FOUND);
                });
        }
    }
}