package kr.hhplus.be.server.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class BalanceChargeRequest {

    @NotNull(message = "충전 금액은 필수입니다.")
    @DecimalMin(value = "1000.0", message = "충전 금액은 1,000원 이상이어야 합니다.")
    private BigDecimal amount;

    public BalanceChargeRequest() {
    }

    public BalanceChargeRequest(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}