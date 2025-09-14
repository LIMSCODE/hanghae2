package kr.hhplus.be.server.order.application;

import kr.hhplus.be.server.domain.Order;
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
import kr.hhplus.be.server.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderUseCase 단위 테스트")
class OrderUseCaseTest {

    @Mock
    private UserPort userPort;

    @Mock
    private ProductPort productPort;

    @Mock
    private OrderPort orderPort;

    @Mock
    private EventPort eventPort;

    @Mock
    private UserService userService;

    @InjectMocks
    private OrderUseCase orderUseCase;

    private User testUser;
    private Product testProduct;
    private OrderRequest orderRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .userId(1L)
                .name("테스트유저")
                .balance(BigDecimal.valueOf(50000))
                .build();

        testProduct = Product.builder()
                .productId(1L)
                .name("테스트상품")
                .price(BigDecimal.valueOf(10000))
                .stockQuantity(100)
                .isActive(true)
                .build();

        OrderRequest.OrderItemRequest itemRequest = new OrderRequest.OrderItemRequest();
        itemRequest.setProductId(1L);
        itemRequest.setQuantity(2);

        orderRequest = new OrderRequest();
        orderRequest.setUserId(1L);
        orderRequest.setOrderItems(List.of(itemRequest));
    }

    @Test
    @DisplayName("주문 생성 성공 - 모든 조건이 만족될 때")
    void createOrder_Success() {
        // given
        Order expectedOrder = new Order(1L, BigDecimal.valueOf(20000));
        expectedOrder.setOrderId(1L);

        given(userPort.getUserWithLock(1L)).willReturn(testUser);
        given(productPort.getProductWithLock(1L)).willReturn(testProduct);
        given(orderPort.saveOrder(any(Order.class))).willReturn(expectedOrder);

        // when
        OrderResponse result = orderUseCase.createOrder(orderRequest);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getOrderId()).isEqualTo(1L);
        assertThat(result.getTotalAmount()).isEqualTo(BigDecimal.valueOf(20000));

        // 모든 포트가 올바르게 호출되었는지 검증
        then(userPort).should().getUserWithLock(1L);
        then(productPort).should().getProductWithLock(1L);
        then(productPort).should().validateStock(testProduct, 2);
        then(productPort).should().deductStock(testProduct, 2);
        then(userPort).should().processPayment(eq(testUser), eq(BigDecimal.valueOf(20000)), isNull());
        then(orderPort).should().saveOrder(any(Order.class));
        then(eventPort).should().publishOrderCompletedEvent(any());
    }

    @Test
    @DisplayName("주문 생성 실패 - 잔액 부족")
    void createOrder_InsufficientBalance() {
        // given
        User poorUser = User.builder()
                .userId(1L)
                .name("가난한유저")
                .balance(BigDecimal.valueOf(1000)) // 부족한 잔액
                .build();

        given(userPort.getUserWithLock(1L)).willReturn(poorUser);
        given(productPort.getProductWithLock(1L)).willReturn(testProduct);

        // when & then
        assertThatThrownBy(() -> orderUseCase.createOrder(orderRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("잔액이 부족합니다");

        // 재고 차감과 결제는 실행되지 않아야 함
        then(productPort).should(never()).deductStock(any(), anyInt());
        then(userPort).should(never()).processPayment(any(), any(), any());
        then(orderPort).should(never()).saveOrder(any());
        then(eventPort).should(never()).publishOrderCompletedEvent(any());
    }

    @Test
    @DisplayName("주문 생성 실패 - 재고 부족")
    void createOrder_InsufficientStock() {
        // given
        given(userPort.getUserWithLock(1L)).willReturn(testUser);
        given(productPort.getProductWithLock(1L)).willReturn(testProduct);
        willThrow(new BusinessException(ErrorCode.INSUFFICIENT_STOCK, "재고가 부족합니다"))
                .given(productPort).validateStock(testProduct, 2);

        // when & then
        assertThatThrownBy(() -> orderUseCase.createOrder(orderRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("재고가 부족합니다");

        // 재고 차감과 결제는 실행되지 않아야 함
        then(productPort).should(never()).deductStock(any(), anyInt());
        then(userPort).should(never()).processPayment(any(), any(), any());
        then(orderPort).should(never()).saveOrder(any());
        then(eventPort).should(never()).publishOrderCompletedEvent(any());
    }

    @Test
    @DisplayName("주문 조회 성공")
    void getOrder_Success() {
        // given
        Order existingOrder = new Order(1L, BigDecimal.valueOf(20000));
        existingOrder.setOrderId(1L);

        given(orderPort.findOrderById(1L)).willReturn(existingOrder);
        given(userService.getUser(1L)).willReturn(testUser);

        // when
        OrderResponse result = orderUseCase.getOrder(1L);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getOrderId()).isEqualTo(1L);
        assertThat(result.getTotalAmount()).isEqualTo(BigDecimal.valueOf(20000));

        then(orderPort).should().findOrderById(1L);
        then(userService).should().getUser(1L);
    }

    @Test
    @DisplayName("주문 조회 실패 - 존재하지 않는 주문")
    void getOrder_NotFound() {
        // given
        given(orderPort.findOrderById(999L)).willReturn(null);

        // when & then
        assertThatThrownBy(() -> orderUseCase.getOrder(999L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ORDER_NOT_FOUND);

        then(orderPort).should().findOrderById(999L);
        then(userService).should(never()).getUser(anyLong());
    }

    @Test
    @DisplayName("복수 상품 주문 성공")
    void createOrder_MultipleProducts_Success() {
        // given
        Product product2 = Product.builder()
                .productId(2L)
                .name("테스트상품2")
                .price(BigDecimal.valueOf(15000))
                .stockQuantity(50)
                .isActive(true)
                .build();

        OrderRequest.OrderItemRequest item1 = new OrderRequest.OrderItemRequest();
        item1.setProductId(1L);
        item1.setQuantity(1);

        OrderRequest.OrderItemRequest item2 = new OrderRequest.OrderItemRequest();
        item2.setProductId(2L);
        item2.setQuantity(2);

        OrderRequest multiItemRequest = new OrderRequest();
        multiItemRequest.setUserId(1L);
        multiItemRequest.setOrderItems(List.of(item1, item2));

        Order expectedOrder = new Order(1L, BigDecimal.valueOf(40000)); // 10000*1 + 15000*2
        expectedOrder.setOrderId(1L);

        given(userPort.getUserWithLock(1L)).willReturn(testUser);
        given(productPort.getProductWithLock(1L)).willReturn(testProduct);
        given(productPort.getProductWithLock(2L)).willReturn(product2);
        given(orderPort.saveOrder(any(Order.class))).willReturn(expectedOrder);

        // when
        OrderResponse result = orderUseCase.createOrder(multiItemRequest);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getTotalAmount()).isEqualTo(BigDecimal.valueOf(40000));

        // 각 상품에 대해 재고 검증 및 차감이 수행되었는지 확인
        then(productPort).should().validateStock(testProduct, 1);
        then(productPort).should().validateStock(product2, 2);
        then(productPort).should().deductStock(testProduct, 1);
        then(productPort).should().deductStock(product2, 2);
    }
}