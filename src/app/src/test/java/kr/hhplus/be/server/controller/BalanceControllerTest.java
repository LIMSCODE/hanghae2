package kr.hhplus.be.server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.hhplus.be.server.dto.BalanceChargeRequest;
import kr.hhplus.be.server.dto.BalanceResponse;
import kr.hhplus.be.server.exception.BusinessException;
import kr.hhplus.be.server.exception.ErrorCode;
import kr.hhplus.be.server.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BalanceController.class)
@DisplayName("BalanceController 테스트")
class BalanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @Test
    @DisplayName("정상적인 잔액 충전")
    void chargeBalance_Success() throws Exception {
        // given
        Long userId = 1L;
        BigDecimal chargeAmount = BigDecimal.valueOf(10000);
        BalanceChargeRequest request = new BalanceChargeRequest(chargeAmount);
        
        BalanceResponse response = new BalanceResponse(
            userId, 
            BigDecimal.valueOf(15000), 
            chargeAmount, 
            LocalDateTime.now()
        );
        
        given(userService.chargeBalance(eq(userId), eq(chargeAmount))).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/v1/users/{userId}/balance/charge", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("잔액 충전이 완료되었습니다"))
                .andExpect(jsonPath("$.data.userId").value(userId))
                .andExpect(jsonPath("$.data.balance").value(15000))
                .andExpect(jsonPath("$.data.chargedAmount").value(10000));
    }

    @Test
    @DisplayName("잘못된 충전 금액으로 요청 시 검증 오류")
    void chargeBalance_InvalidAmount_ValidationError() throws Exception {
        // given
        Long userId = 1L;
        BalanceChargeRequest request = new BalanceChargeRequest(BigDecimal.valueOf(500)); // 최소 금액 미만

        // when & then
        mockMvc.perform(post("/api/v1/users/{userId}/balance/charge", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"))
                .andExpect(jsonPath("$.message").value("입력값 검증에 실패했습니다"))
                .andExpect(jsonPath("$.data.amount").exists());
    }

    @Test
    @DisplayName("존재하지 않는 사용자 충전 시 오류")
    void chargeBalance_UserNotFound_Error() throws Exception {
        // given
        Long userId = 999L;
        BalanceChargeRequest request = new BalanceChargeRequest(BigDecimal.valueOf(10000));
        
        given(userService.chargeBalance(eq(userId), any(BigDecimal.class)))
            .willThrow(new BusinessException(ErrorCode.USER_NOT_FOUND));

        // when & then
        mockMvc.perform(post("/api/v1/users/{userId}/balance/charge", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
    }

    @Test
    @DisplayName("정상적인 잔액 조회")
    void getBalance_Success() throws Exception {
        // given
        Long userId = 1L;
        BalanceResponse response = new BalanceResponse(
            userId, 
            BigDecimal.valueOf(25000), 
            LocalDateTime.now()
        );
        
        given(userService.getBalance(userId)).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/v1/users/{userId}/balance", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("잔액 조회가 완료되었습니다"))
                .andExpect(jsonPath("$.data.userId").value(userId))
                .andExpect(jsonPath("$.data.balance").value(25000))
                .andExpect(jsonPath("$.data.lastUpdatedAt").exists());
    }

    @Test
    @DisplayName("존재하지 않는 사용자 조회 시 오류")
    void getBalance_UserNotFound_Error() throws Exception {
        // given
        Long userId = 999L;
        
        given(userService.getBalance(userId))
            .willThrow(new BusinessException(ErrorCode.USER_NOT_FOUND));

        // when & then
        mockMvc.perform(get("/api/v1/users/{userId}/balance", userId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
    }
}