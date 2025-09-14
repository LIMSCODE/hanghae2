package kr.hhplus.be.server.controller;

import kr.hhplus.be.server.domain.Coupon;
import kr.hhplus.be.server.domain.UserCoupon;
import kr.hhplus.be.server.dto.ApiResponse;
import kr.hhplus.be.server.dto.CouponResponse;
import kr.hhplus.be.server.dto.UserCouponResponse;
import kr.hhplus.be.server.service.CouponService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/coupons")
public class CouponController {

    private final CouponService couponService;

    public CouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    /**
     * 현재 발급 가능한 쿠폰 목록 조회
     */
    @GetMapping("/available")
    public ResponseEntity<ApiResponse<List<CouponResponse>>> getAvailableCoupons() {
        List<Coupon> coupons = couponService.getAvailableCoupons();
        List<CouponResponse> responses = coupons.stream()
                .map(CouponResponse::new)
                .collect(Collectors.toList());

        return ResponseEntity.ok(
            ApiResponse.success("발급 가능한 쿠폰 목록 조회가 완료되었습니다", responses)
        );
    }

    /**
     * 선착순 쿠폰 발급
     */
    @PostMapping("/{couponId}/issue")
    public ResponseEntity<ApiResponse<UserCouponResponse>> issueCoupon(
            @PathVariable Long couponId,
            @RequestParam Long userId) {

        UserCoupon userCoupon = couponService.issueCoupon(userId, couponId);
        UserCouponResponse response = new UserCouponResponse(userCoupon);

        return ResponseEntity.ok(
            ApiResponse.success("쿠폰이 성공적으로 발급되었습니다", response)
        );
    }

    /**
     * 사용자의 보유 쿠폰 목록 조회
     */
    @GetMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<List<UserCouponResponse>>> getUserCoupons(
            @PathVariable Long userId) {

        List<UserCoupon> userCoupons = couponService.getUserCoupons(userId);
        List<UserCouponResponse> responses = userCoupons.stream()
                .map(UserCouponResponse::new)
                .collect(Collectors.toList());

        return ResponseEntity.ok(
            ApiResponse.success("사용자 쿠폰 목록 조회가 완료되었습니다", responses)
        );
    }

    /**
     * 쿠폰 상세 정보 조회
     */
    @GetMapping("/{couponId}")
    public ResponseEntity<ApiResponse<CouponResponse>> getCoupon(@PathVariable Long couponId) {
        Coupon coupon = couponService.getCoupon(couponId);
        CouponResponse response = new CouponResponse(coupon);

        return ResponseEntity.ok(
            ApiResponse.success("쿠폰 상세 정보 조회가 완료되었습니다", response)
        );
    }

    /**
     * 모든 쿠폰 목록 조회 (관리용)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<CouponResponse>>> getAllCoupons() {
        List<Coupon> coupons = couponService.getAllCoupons();
        List<CouponResponse> responses = coupons.stream()
                .map(CouponResponse::new)
                .collect(Collectors.toList());

        return ResponseEntity.ok(
            ApiResponse.success("전체 쿠폰 목록 조회가 완료되었습니다", responses)
        );
    }
}