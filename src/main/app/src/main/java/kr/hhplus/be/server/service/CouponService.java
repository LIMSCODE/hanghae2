package kr.hhplus.be.server.service;

import kr.hhplus.be.server.domain.Coupon;
import kr.hhplus.be.server.domain.User;
import kr.hhplus.be.server.domain.UserCoupon;
import kr.hhplus.be.server.exception.BusinessException;
import kr.hhplus.be.server.exception.ErrorCode;
import kr.hhplus.be.server.repository.CouponRepository;
import kr.hhplus.be.server.repository.UserCouponRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class CouponService {

    private static final Logger logger = LoggerFactory.getLogger(CouponService.class);

    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;
    private final UserService userService;

    public CouponService(CouponRepository couponRepository,
                        UserCouponRepository userCouponRepository,
                        UserService userService) {
        this.couponRepository = couponRepository;
        this.userCouponRepository = userCouponRepository;
        this.userService = userService;
    }

    /**
     * 선착순 쿠폰 발급 (동시성 처리)
     */
    @Transactional
    public UserCoupon issueCoupon(Long userId, Long couponId) {
        // 1. 사용자 존재 확인
        User user = userService.getUser(userId);

        // 2. 이미 발급받은 쿠폰인지 확인
        if (userCouponRepository.existsByUserIdAndCouponId(userId, couponId)) {
            throw new BusinessException(ErrorCode.COUPON_ALREADY_ISSUED);
        }

        // 3. 쿠폰 조회 및 비관적 락 적용 (선착순 처리를 위한 동시성 제어)
        Coupon coupon = couponRepository.findByIdWithLock(couponId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COUPON_NOT_FOUND));

        // 4. 쿠폰 발급 가능 여부 검증 및 발급 수량 증가
        coupon.issueCoupon();

        // 5. 사용자-쿠폰 관계 생성
        UserCoupon userCoupon = new UserCoupon(userId, couponId);
        UserCoupon savedUserCoupon = userCouponRepository.save(userCoupon);

        logger.info("Coupon issued successfully. CouponId: {}, UserId: {}, AvailableQuantity: {}",
                   couponId, userId, coupon.getAvailableQuantity());

        return savedUserCoupon;
    }

    /**
     * 사용자의 보유 쿠폰 목록 조회
     */
    public List<UserCoupon> getUserCoupons(Long userId) {
        userService.getUser(userId); // 사용자 존재 확인
        return userCouponRepository.findByUserIdAndIsUsedFalse(userId);
    }

    /**
     * 현재 발급 가능한 쿠폰 목록 조회
     */
    public List<Coupon> getAvailableCoupons() {
        return couponRepository.findAvailableCoupons(LocalDateTime.now());
    }

    /**
     * 모든 쿠폰 목록 조회 (관리용)
     */
    public List<Coupon> getAllCoupons() {
        return couponRepository.findByIsActiveTrue();
    }

    /**
     * 쿠폰 상세 정보 조회
     */
    public Coupon getCoupon(Long couponId) {
        return couponRepository.findById(couponId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COUPON_NOT_FOUND));
    }

    /**
     * 쿠폰 사용 (주문 시 호출)
     */
    @Transactional
    public void useCoupon(Long userId, Long couponId) {
        UserCoupon userCoupon = userCouponRepository.findByUserIdAndCouponId(userId, couponId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COUPON_NOT_FOUND, "보유하지 않은 쿠폰입니다"));

        if (userCoupon.getIsUsed()) {
            throw new BusinessException(ErrorCode.COUPON_ALREADY_ISSUED, "이미 사용된 쿠폰입니다");
        }

        userCoupon.useCoupon();
        logger.info("Coupon used. UserCouponId: {}, UserId: {}, CouponId: {}",
                   userCoupon.getUserCouponId(), userId, couponId);
    }
}