package kr.hhplus.be.server.controller;

import kr.hhplus.be.server.dto.ApiResponse;
import kr.hhplus.be.server.dto.BalanceChargeRequest;
import kr.hhplus.be.server.dto.BalanceResponse;
import kr.hhplus.be.server.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/users/{userId}/balance")
@Validated
public class BalanceController {

    private final UserService userService;

    public BalanceController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/charge")
    public ResponseEntity<ApiResponse<BalanceResponse>> chargeBalance(
            @PathVariable Long userId,
            @Valid @RequestBody BalanceChargeRequest request) {
        
        BalanceResponse response = userService.chargeBalance(userId, request.getAmount());
        
        return ResponseEntity.ok(
            ApiResponse.success("잔액 충전이 완료되었습니다", response)
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<BalanceResponse>> getBalance(@PathVariable Long userId) {
        
        BalanceResponse response = userService.getBalance(userId);
        
        return ResponseEntity.ok(
            ApiResponse.success("잔액 조회가 완료되었습니다", response)
        );
    }
}