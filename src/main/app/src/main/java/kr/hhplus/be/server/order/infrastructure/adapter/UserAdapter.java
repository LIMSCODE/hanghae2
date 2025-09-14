package kr.hhplus.be.server.order.infrastructure.adapter;

import kr.hhplus.be.server.domain.User;
import kr.hhplus.be.server.order.domain.port.UserPort;
import kr.hhplus.be.server.service.UserService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class UserAdapter implements UserPort {

    private final UserService userService;

    public UserAdapter(UserService userService) {
        this.userService = userService;
    }

    @Override
    public User getUserWithLock(Long userId) {
        return userService.getUserWithLock(userId);
    }

    @Override
    public void processPayment(User user, BigDecimal amount, Long orderId) {
        userService.processPayment(user, amount, orderId);
    }
}