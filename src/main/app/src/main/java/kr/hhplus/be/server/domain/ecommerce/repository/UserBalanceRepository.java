package kr.hhplus.be.server.domain.ecommerce.repository;

import kr.hhplus.be.server.domain.User;
import kr.hhplus.be.server.domain.BalanceHistory;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface UserBalanceRepository {
    User save(User user);
    Optional<User> findById(Long userId);
    Optional<User> findByIdWithLock(Long userId);

    BalanceHistory save(BalanceHistory balanceHistory);
    List<BalanceHistory> findByUserIdOrderByCreatedAtDesc(Long userId);

    void chargeBalance(Long userId, BigDecimal amount);
    void deductBalance(Long userId, BigDecimal amount);
    BigDecimal getBalance(Long userId);
}