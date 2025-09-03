package kr.hhplus.be.server.service;

import kr.hhplus.be.server.domain.BalanceHistory;
import kr.hhplus.be.server.domain.User;
import kr.hhplus.be.server.dto.BalanceResponse;
import kr.hhplus.be.server.exception.BusinessException;
import kr.hhplus.be.server.exception.ErrorCode;
import kr.hhplus.be.server.repository.BalanceHistoryRepository;
import kr.hhplus.be.server.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final BalanceHistoryRepository balanceHistoryRepository;

    public UserService(UserRepository userRepository, BalanceHistoryRepository balanceHistoryRepository) {
        this.userRepository = userRepository;
        this.balanceHistoryRepository = balanceHistoryRepository;
    }

    @Transactional
    public BalanceResponse chargeBalance(Long userId, BigDecimal amount) {
        validateChargeAmount(amount);
        
        User user = userRepository.findByIdWithLock(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        BigDecimal balanceBefore = user.getBalance();
        user.chargeBalance(amount);
        
        User savedUser = userRepository.save(user);
        
        BalanceHistory history = BalanceHistory.createChargeHistory(
            userId, amount, balanceBefore, savedUser.getBalance()
        );
        balanceHistoryRepository.save(history);

        return new BalanceResponse(userId, savedUser.getBalance(), amount, history.getCreatedAt());
    }

    public BalanceResponse getBalance(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        return new BalanceResponse(userId, user.getBalance(), user.getUpdatedAt());
    }

    @Transactional
    public User getUserWithLock(Long userId) {
        return userRepository.findByIdWithLock(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    public User getUser(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    @Transactional
    public void processPayment(User user, BigDecimal amount, Long orderId) {
        BigDecimal balanceBefore = user.getBalance();
        user.deductBalance(amount);
        
        User savedUser = userRepository.save(user);
        
        BalanceHistory history = BalanceHistory.createPaymentHistory(
            user.getUserId(), amount, balanceBefore, savedUser.getBalance(), orderId
        );
        balanceHistoryRepository.save(history);
    }

    private void validateChargeAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.valueOf(1000)) < 0) {
            throw new BusinessException(ErrorCode.INVALID_AMOUNT, "충전 금액은 1,000원 이상이어야 합니다.");
        }
        if (amount.compareTo(BigDecimal.valueOf(1_000_000)) > 0) {
            throw new BusinessException(ErrorCode.INVALID_AMOUNT, "1회 충전 한도는 1,000,000원입니다.");
        }
    }
}