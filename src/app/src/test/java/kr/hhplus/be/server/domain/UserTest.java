package kr.hhplus.be.server.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

@DisplayName("User 도메인 테스트")
class UserTest {

    @Nested
    @DisplayName("잔액 충전 테스트")
    class ChargeBalanceTest {

        @Test
        @DisplayName("정상적인 잔액 충전")
        void chargeBalance_Success() {
            // given
            User user = new User("testUser", "test@example.com");
            BigDecimal chargeAmount = BigDecimal.valueOf(10000);
            BigDecimal expectedBalance = BigDecimal.valueOf(10000);

            // when
            user.chargeBalance(chargeAmount);

            // then
            assertThat(user.getBalance()).isEqualTo(expectedBalance);
        }

        @Test
        @DisplayName("기존 잔액에 추가 충전")
        void chargeBalance_AddToExisting() {
            // given
            User user = new User("testUser", "test@example.com");
            user.chargeBalance(BigDecimal.valueOf(5000));
            BigDecimal additionalCharge = BigDecimal.valueOf(3000);
            BigDecimal expectedBalance = BigDecimal.valueOf(8000);

            // when
            user.chargeBalance(additionalCharge);

            // then
            assertThat(user.getBalance()).isEqualTo(expectedBalance);
        }

        @Test
        @DisplayName("null 금액으로 충전 시 예외 발생")
        void chargeBalance_NullAmount_ThrowsException() {
            // given
            User user = new User("testUser", "test@example.com");

            // when & then
            assertThatThrownBy(() -> user.chargeBalance(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("충전 금액은 0보다 커야 합니다.");
        }

        @Test
        @DisplayName("0원 충전 시 예외 발생")
        void chargeBalance_ZeroAmount_ThrowsException() {
            // given
            User user = new User("testUser", "test@example.com");

            // when & then
            assertThatThrownBy(() -> user.chargeBalance(BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("충전 금액은 0보다 커야 합니다.");
        }

        @Test
        @DisplayName("음수 금액 충전 시 예외 발생")
        void chargeBalance_NegativeAmount_ThrowsException() {
            // given
            User user = new User("testUser", "test@example.com");

            // when & then
            assertThatThrownBy(() -> user.chargeBalance(BigDecimal.valueOf(-1000)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("충전 금액은 0보다 커야 합니다.");
        }
    }

    @Nested
    @DisplayName("잔액 차감 테스트")
    class DeductBalanceTest {

        @Test
        @DisplayName("정상적인 잔액 차감")
        void deductBalance_Success() {
            // given
            User user = new User("testUser", "test@example.com");
            user.chargeBalance(BigDecimal.valueOf(10000));
            BigDecimal deductAmount = BigDecimal.valueOf(3000);
            BigDecimal expectedBalance = BigDecimal.valueOf(7000);

            // when
            user.deductBalance(deductAmount);

            // then
            assertThat(user.getBalance()).isEqualTo(expectedBalance);
        }

        @Test
        @DisplayName("잔액 부족 시 예외 발생")
        void deductBalance_InsufficientBalance_ThrowsException() {
            // given
            User user = new User("testUser", "test@example.com");
            user.chargeBalance(BigDecimal.valueOf(5000));
            BigDecimal deductAmount = BigDecimal.valueOf(10000);

            // when & then
            assertThatThrownBy(() -> user.deductBalance(deductAmount))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("잔액이 부족합니다. 현재 잔액: 5000");
        }

        @Test
        @DisplayName("null 금액 차감 시 예외 발생")
        void deductBalance_NullAmount_ThrowsException() {
            // given
            User user = new User("testUser", "test@example.com");

            // when & then
            assertThatThrownBy(() -> user.deductBalance(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("차감 금액은 0보다 커야 합니다.");
        }
    }

    @Nested
    @DisplayName("잔액 충분성 검사 테스트")
    class HasEnoughBalanceTest {

        @Test
        @DisplayName("잔액이 충분한 경우")
        void hasEnoughBalance_Sufficient() {
            // given
            User user = new User("testUser", "test@example.com");
            user.chargeBalance(BigDecimal.valueOf(10000));

            // when & then
            assertThat(user.hasEnoughBalance(BigDecimal.valueOf(5000))).isTrue();
            assertThat(user.hasEnoughBalance(BigDecimal.valueOf(10000))).isTrue();
        }

        @Test
        @DisplayName("잔액이 부족한 경우")
        void hasEnoughBalance_Insufficient() {
            // given
            User user = new User("testUser", "test@example.com");
            user.chargeBalance(BigDecimal.valueOf(5000));

            // when & then
            assertThat(user.hasEnoughBalance(BigDecimal.valueOf(10000))).isFalse();
        }
    }
}