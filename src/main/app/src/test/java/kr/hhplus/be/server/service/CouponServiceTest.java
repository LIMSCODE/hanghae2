package kr.hhplus.be.server.service;

import kr.hhplus.be.server.domain.Coupon;
import kr.hhplus.be.server.domain.User;
import kr.hhplus.be.server.domain.UserCoupon;
import kr.hhplus.be.server.exception.BusinessException;
import kr.hhplus.be.server.exception.ErrorCode;
import kr.hhplus.be.server.repository.CouponRepository;
import kr.hhplus.be.server.repository.UserCouponRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CouponService 단위 테스트")
class CouponServiceTest {

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private UserCouponRepository userCouponRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private CouponService couponService;

    private User testUser;
    private Coupon testCoupon;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .userId(1L)
                .name("테스트유저")
                .balance(BigDecimal.valueOf(50000))
                .build();

        testCoupon = new Coupon(
                "선착순 할인 쿠폰",
                BigDecimal.valueOf(5000),
                100,
                LocalDateTime.now().minusHours(1),
                LocalDateTime.now().plusHours(1)
        );
    }

    @Test
    @DisplayName("선착순 쿠폰 발급 성공 - 정상적인 조건에서")
    void issueCoupon_Success() {
        // given
        Long userId = 1L;
        Long couponId = 1L;

        given(userService.getUser(userId)).willReturn(testUser);
        given(userCouponRepository.existsByUserIdAndCouponId(userId, couponId)).willReturn(false);
        given(couponRepository.findByIdWithLock(couponId)).willReturn(Optional.of(testCoupon));
        given(userCouponRepository.save(any(UserCoupon.class))).willReturn(new UserCoupon(userId, couponId));

        // when
        UserCoupon result = couponService.issueCoupon(userId, couponId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getCouponId()).isEqualTo(couponId);

        // 쿠폰 발급 수량이 증가했는지 확인
        assertThat(testCoupon.getIssuedQuantity()).isEqualTo(1);

        // Mock 호출 검증
        then(userService).should().getUser(userId);
        then(userCouponRepository).should().existsByUserIdAndCouponId(userId, couponId);
        then(couponRepository).should().findByIdWithLock(couponId);
        then(userCouponRepository).should().save(any(UserCoupon.class));
    }

    @Test
    @DisplayName("선착순 쿠폰 발급 실패 - 이미 발급받은 쿠폰")
    void issueCoupon_AlreadyIssued() {
        // given
        Long userId = 1L;
        Long couponId = 1L;

        given(userService.getUser(userId)).willReturn(testUser);
        given(userCouponRepository.existsByUserIdAndCouponId(userId, couponId)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> couponService.issueCoupon(userId, couponId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.COUPON_ALREADY_ISSUED);

        // 쿠폰 조회나 발급 처리가 되지 않아야 함
        then(couponRepository).should(never()).findByIdWithLock(any());
        then(userCouponRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("선착순 쿠폰 발급 실패 - 존재하지 않는 쿠폰")
    void issueCoupon_CouponNotFound() {
        // given
        Long userId = 1L;
        Long couponId = 999L;

        given(userService.getUser(userId)).willReturn(testUser);
        given(userCouponRepository.existsByUserIdAndCouponId(userId, couponId)).willReturn(false);
        given(couponRepository.findByIdWithLock(couponId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> couponService.issueCoupon(userId, couponId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.COUPON_NOT_FOUND);

        then(userCouponRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("선착순 쿠폰 발급 실패 - 쿠폰 소진")
    void issueCoupon_CouponSoldOut() {
        // given
        Long userId = 1L;
        Long couponId = 1L;

        // 쿠폰을 모두 소진된 상태로 설정
        Coupon soldOutCoupon = new Coupon(
                "매진된 쿠폰",
                BigDecimal.valueOf(5000),
                100,
                LocalDateTime.now().minusHours(1),
                LocalDateTime.now().plusHours(1)
        );
        // 발급 수량을 총 수량과 같게 설정 (매진)
        for (int i = 0; i < 100; i++) {
            soldOutCoupon.issueCoupon(); // 100개 모두 발급
        }

        given(userService.getUser(userId)).willReturn(testUser);
        given(userCouponRepository.existsByUserIdAndCouponId(userId, couponId)).willReturn(false);
        given(couponRepository.findByIdWithLock(couponId)).willReturn(Optional.of(soldOutCoupon));

        // when & then
        assertThatThrownBy(() -> couponService.issueCoupon(userId, couponId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.COUPON_SOLD_OUT);

        then(userCouponRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("사용자 보유 쿠폰 목록 조회 성공")
    void getUserCoupons_Success() {
        // given
        Long userId = 1L;
        List<UserCoupon> userCoupons = List.of(
                new UserCoupon(userId, 1L),
                new UserCoupon(userId, 2L)
        );

        given(userService.getUser(userId)).willReturn(testUser);
        given(userCouponRepository.findByUserIdAndIsUsedFalse(userId)).willReturn(userCoupons);

        // when
        List<UserCoupon> result = couponService.getUserCoupons(userId);

        // then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(UserCoupon::getUserId)
                .containsOnly(userId);

        then(userService).should().getUser(userId);
        then(userCouponRepository).should().findByUserIdAndIsUsedFalse(userId);
    }

    @Test
    @DisplayName("발급 가능한 쿠폰 목록 조회 성공")
    void getAvailableCoupons_Success() {
        // given
        List<Coupon> availableCoupons = List.of(testCoupon);
        given(couponRepository.findAvailableCoupons(any(LocalDateTime.class))).willReturn(availableCoupons);

        // when
        List<Coupon> result = couponService.getAvailableCoupons();

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("선착순 할인 쿠폰");

        then(couponRepository).should().findAvailableCoupons(any(LocalDateTime.class));
    }

    @Test
    @DisplayName("쿠폰 사용 성공")
    void useCoupon_Success() {
        // given
        Long userId = 1L;
        Long couponId = 1L;
        UserCoupon userCoupon = new UserCoupon(userId, couponId);

        given(userCouponRepository.findByUserIdAndCouponId(userId, couponId))
                .willReturn(Optional.of(userCoupon));

        // when
        couponService.useCoupon(userId, couponId);

        // then
        assertThat(userCoupon.getIsUsed()).isTrue();
        assertThat(userCoupon.getUsedAt()).isNotNull();

        then(userCouponRepository).should().findByUserIdAndCouponId(userId, couponId);
    }

    @Test
    @DisplayName("쿠폰 사용 실패 - 보유하지 않은 쿠폰")
    void useCoupon_NotOwned() {
        // given
        Long userId = 1L;
        Long couponId = 1L;

        given(userCouponRepository.findByUserIdAndCouponId(userId, couponId))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> couponService.useCoupon(userId, couponId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("보유하지 않은 쿠폰입니다");
    }
}