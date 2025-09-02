package kr.hhplus.be.server.service;

import kr.hhplus.be.server.domain.BalanceHistory;
import kr.hhplus.be.server.domain.User;
import kr.hhplus.be.server.dto.BalanceResponse;
import kr.hhplus.be.server.exception.BusinessException;
import kr.hhplus.be.server.exception.ErrorCode;
import kr.hhplus.be.server.repository.BalanceHistoryRepository;
import kr.hhplus.be.server.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService 테스트")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private BalanceHistoryRepository balanceHistoryRepository;

    @InjectMocks
    private UserService userService;

    @Nested
    @DisplayName("잔액 충전 테스트")
    class ChargeBalanceTest {

        @Test
        @DisplayName("정상적인 잔액 충전")
        void chargeBalance_Success() {
            // given
            Long userId = 1L;
            BigDecimal chargeAmount = BigDecimal.valueOf(10000);
            User user = new User("testUser", "test@example.com");
            
            given(userRepository.findByIdWithLock(userId)).willReturn(Optional.of(user));
            given(userRepository.save(any(User.class))).willReturn(user);
            given(balanceHistoryRepository.save(any(BalanceHistory.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

            // when
            BalanceResponse response = userService.chargeBalance(userId, chargeAmount);

            // then
            assertThat(response.getUserId()).isEqualTo(userId);
            assertThat(response.getBalance()).isEqualTo(chargeAmount);
            assertThat(response.getChargedAmount()).isEqualTo(chargeAmount);
            
            verify(userRepository).findByIdWithLock(userId);
            verify(userRepository).save(user);
            verify(balanceHistoryRepository).save(any(BalanceHistory.class));
        }

        @Test
        @DisplayName("존재하지 않는 사용자 충전 시 예외 발생")
        void chargeBalance_UserNotFound_ThrowsException() {
            // given
            Long userId = 1L;
            BigDecimal chargeAmount = BigDecimal.valueOf(10000);
            
            given(userRepository.findByIdWithLock(userId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> userService.chargeBalance(userId, chargeAmount))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> {
                    BusinessException be = (BusinessException) exception;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
                });
        }

        @Test
        @DisplayName("최소 충전 금액 미만 충전 시 예외 발생")
        void chargeBalance_InvalidAmount_ThrowsException() {
            // given
            Long userId = 1L;
            BigDecimal invalidAmount = BigDecimal.valueOf(500);

            // when & then
            assertThatThrownBy(() -> userService.chargeBalance(userId, invalidAmount))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> {
                    BusinessException be = (BusinessException) exception;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.INVALID_AMOUNT);
                    assertThat(be.getMessage()).contains("충전 금액은 1,000원 이상이어야 합니다");
                });
        }

        @Test
        @DisplayName("최대 충전 한도 초과 시 예외 발생")
        void chargeBalance_ExceedsLimit_ThrowsException() {
            // given
            Long userId = 1L;
            BigDecimal excessiveAmount = BigDecimal.valueOf(2_000_000);

            // when & then
            assertThatThrownBy(() -> userService.chargeBalance(userId, excessiveAmount))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> {
                    BusinessException be = (BusinessException) exception;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.INVALID_AMOUNT);
                    assertThat(be.getMessage()).contains("1회 충전 한도는 1,000,000원입니다");
                });
        }
    }

    @Nested
    @DisplayName("잔액 조회 테스트")
    class GetBalanceTest {

        @Test
        @DisplayName("정상적인 잔액 조회")
        void getBalance_Success() {
            // given
            Long userId = 1L;
            User user = new User("testUser", "test@example.com");
            user.chargeBalance(BigDecimal.valueOf(50000));
            
            given(userRepository.findById(userId)).willReturn(Optional.of(user));

            // when
            BalanceResponse response = userService.getBalance(userId);

            // then
            assertThat(response.getUserId()).isEqualTo(userId);
            assertThat(response.getBalance()).isEqualTo(BigDecimal.valueOf(50000));
            assertThat(response.getLastUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("존재하지 않는 사용자 조회 시 예외 발생")
        void getBalance_UserNotFound_ThrowsException() {
            // given
            Long userId = 1L;
            
            given(userRepository.findById(userId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> userService.getBalance(userId))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> {
                    BusinessException be = (BusinessException) exception;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
                });
        }
    }

    @Nested
    @DisplayName("결제 처리 테스트")
    class ProcessPaymentTest {

        @Test
        @DisplayName("정상적인 결제 처리")
        void processPayment_Success() {
            // given
            User user = new User("testUser", "test@example.com");
            user.chargeBalance(BigDecimal.valueOf(20000));
            BigDecimal paymentAmount = BigDecimal.valueOf(15000);
            Long orderId = 1L;
            
            given(userRepository.save(any(User.class))).willReturn(user);
            given(balanceHistoryRepository.save(any(BalanceHistory.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

            // when
            userService.processPayment(user, paymentAmount, orderId);

            // then
            assertThat(user.getBalance()).isEqualTo(BigDecimal.valueOf(5000));
            
            verify(userRepository).save(user);
            verify(balanceHistoryRepository).save(any(BalanceHistory.class));
        }
    }

    @Nested
    @DisplayName("사용자 조회 테스트")
    class GetUserTest {

        @Test
        @DisplayName("락과 함께 사용자 조회")
        void getUserWithLock_Success() {
            // given
            Long userId = 1L;
            User user = new User("testUser", "test@example.com");
            
            given(userRepository.findByIdWithLock(userId)).willReturn(Optional.of(user));

            // when
            User result = userService.getUserWithLock(userId);

            // then
            assertThat(result).isEqualTo(user);
            verify(userRepository).findByIdWithLock(userId);
        }

        @Test
        @DisplayName("일반 사용자 조회")
        void getUser_Success() {
            // given
            Long userId = 1L;
            User user = new User("testUser", "test@example.com");
            
            given(userRepository.findById(userId)).willReturn(Optional.of(user));

            // when
            User result = userService.getUser(userId);

            // then
            assertThat(result).isEqualTo(user);
            verify(userRepository).findById(userId);
        }
    }
}