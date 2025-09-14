package kr.hhplus.be.server.order.domain.port;

import kr.hhplus.be.server.domain.User;

import java.math.BigDecimal;

public interface UserPort {
    User getUserWithLock(Long userId);
    void processPayment(User user, BigDecimal amount, Long orderId);
}