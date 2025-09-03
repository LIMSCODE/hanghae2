package kr.hhplus.be.server.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Product 도메인 테스트")
class ProductTest {

    @Nested
    @DisplayName("재고 차감 테스트")
    class DeductStockTest {

        @Test
        @DisplayName("정상적인 재고 차감")
        void deductStock_Success() {
            // given
            Product product = new Product("iPhone 15", "Apple iPhone", BigDecimal.valueOf(1000000), 10);
            Integer deductQuantity = 3;
            Integer expectedStock = 7;

            // when
            product.deductStock(deductQuantity);

            // then
            assertThat(product.getStockQuantity()).isEqualTo(expectedStock);
        }

        @Test
        @DisplayName("전체 재고 차감")
        void deductStock_AllStock() {
            // given
            Product product = new Product("iPhone 15", "Apple iPhone", BigDecimal.valueOf(1000000), 5);
            Integer deductQuantity = 5;
            Integer expectedStock = 0;

            // when
            product.deductStock(deductQuantity);

            // then
            assertThat(product.getStockQuantity()).isEqualTo(expectedStock);
        }

        @Test
        @DisplayName("재고 부족 시 예외 발생")
        void deductStock_InsufficientStock_ThrowsException() {
            // given
            Product product = new Product("iPhone 15", "Apple iPhone", BigDecimal.valueOf(1000000), 3);
            Integer deductQuantity = 5;

            // when & then
            assertThatThrownBy(() -> product.deductStock(deductQuantity))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("재고가 부족합니다. 요청: 5개, 현재 재고: 3개");
        }

        @Test
        @DisplayName("null 수량 차감 시 예외 발생")
        void deductStock_NullQuantity_ThrowsException() {
            // given
            Product product = new Product("iPhone 15", "Apple iPhone", BigDecimal.valueOf(1000000), 10);

            // when & then
            assertThatThrownBy(() -> product.deductStock(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("차감할 수량은 0보다 커야 합니다.");
        }

        @Test
        @DisplayName("0 수량 차감 시 예외 발생")
        void deductStock_ZeroQuantity_ThrowsException() {
            // given
            Product product = new Product("iPhone 15", "Apple iPhone", BigDecimal.valueOf(1000000), 10);

            // when & then
            assertThatThrownBy(() -> product.deductStock(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("차감할 수량은 0보다 커야 합니다.");
        }
    }

    @Nested
    @DisplayName("재고 추가 테스트")
    class AddStockTest {

        @Test
        @DisplayName("정상적인 재고 추가")
        void addStock_Success() {
            // given
            Product product = new Product("iPhone 15", "Apple iPhone", BigDecimal.valueOf(1000000), 5);
            Integer addQuantity = 10;
            Integer expectedStock = 15;

            // when
            product.addStock(addQuantity);

            // then
            assertThat(product.getStockQuantity()).isEqualTo(expectedStock);
        }

        @Test
        @DisplayName("null 수량 추가 시 예외 발생")
        void addStock_NullQuantity_ThrowsException() {
            // given
            Product product = new Product("iPhone 15", "Apple iPhone", BigDecimal.valueOf(1000000), 10);

            // when & then
            assertThatThrownBy(() -> product.addStock(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("추가할 수량은 0보다 커야 합니다.");
        }
    }

    @Nested
    @DisplayName("재고 충분성 검사 테스트")
    class HasEnoughStockTest {

        @Test
        @DisplayName("재고가 충분한 경우")
        void hasEnoughStock_Sufficient() {
            // given
            Product product = new Product("iPhone 15", "Apple iPhone", BigDecimal.valueOf(1000000), 10);

            // when & then
            assertThat(product.hasEnoughStock(5)).isTrue();
            assertThat(product.hasEnoughStock(10)).isTrue();
        }

        @Test
        @DisplayName("재고가 부족한 경우")
        void hasEnoughStock_Insufficient() {
            // given
            Product product = new Product("iPhone 15", "Apple iPhone", BigDecimal.valueOf(1000000), 5);

            // when & then
            assertThat(product.hasEnoughStock(10)).isFalse();
        }
    }

    @Nested
    @DisplayName("소계 계산 테스트")
    class CalculateSubtotalTest {

        @Test
        @DisplayName("정상적인 소계 계산")
        void calculateSubtotal_Success() {
            // given
            Product product = new Product("iPhone 15", "Apple iPhone", BigDecimal.valueOf(1000000), 10);
            Integer quantity = 3;
            BigDecimal expectedSubtotal = BigDecimal.valueOf(3000000);

            // when
            BigDecimal subtotal = product.calculateSubtotal(quantity);

            // then
            assertThat(subtotal).isEqualTo(expectedSubtotal);
        }

        @Test
        @DisplayName("수량 1개인 경우")
        void calculateSubtotal_QuantityOne() {
            // given
            Product product = new Product("iPhone 15", "Apple iPhone", BigDecimal.valueOf(1000000), 10);
            Integer quantity = 1;
            BigDecimal expectedSubtotal = BigDecimal.valueOf(1000000);

            // when
            BigDecimal subtotal = product.calculateSubtotal(quantity);

            // then
            assertThat(subtotal).isEqualTo(expectedSubtotal);
        }

        @Test
        @DisplayName("null 수량으로 계산 시 예외 발생")
        void calculateSubtotal_NullQuantity_ThrowsException() {
            // given
            Product product = new Product("iPhone 15", "Apple iPhone", BigDecimal.valueOf(1000000), 10);

            // when & then
            assertThatThrownBy(() -> product.calculateSubtotal(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("수량은 0보다 커야 합니다.");
        }

        @Test
        @DisplayName("0 수량으로 계산 시 예외 발생")
        void calculateSubtotal_ZeroQuantity_ThrowsException() {
            // given
            Product product = new Product("iPhone 15", "Apple iPhone", BigDecimal.valueOf(1000000), 10);

            // when & then
            assertThatThrownBy(() -> product.calculateSubtotal(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("수량은 0보다 커야 합니다.");
        }
    }

    @Nested
    @DisplayName("상품 활성화/비활성화 테스트")
    class ActivationTest {

        @Test
        @DisplayName("상품 비활성화")
        void deactivate_Success() {
            // given
            Product product = new Product("iPhone 15", "Apple iPhone", BigDecimal.valueOf(1000000), 10);
            
            // when
            product.deactivate();

            // then
            assertThat(product.getIsActive()).isFalse();
        }

        @Test
        @DisplayName("상품 활성화")
        void activate_Success() {
            // given
            Product product = new Product("iPhone 15", "Apple iPhone", BigDecimal.valueOf(1000000), 10);
            product.deactivate();  // 먼저 비활성화
            
            // when
            product.activate();

            // then
            assertThat(product.getIsActive()).isTrue();
        }
    }
}