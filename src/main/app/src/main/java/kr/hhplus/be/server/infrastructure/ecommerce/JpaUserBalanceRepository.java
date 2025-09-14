package kr.hhplus.be.server.infrastructure.ecommerce;

import kr.hhplus.be.server.domain.User;
import kr.hhplus.be.server.domain.BalanceHistory;
import kr.hhplus.be.server.domain.ecommerce.repository.UserBalanceRepository;
import kr.hhplus.be.server.repository.UserRepository;
import kr.hhplus.be.server.repository.BalanceHistoryRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public class JpaUserBalanceRepository implements UserBalanceRepository {

    private final UserRepository userRepository;
    private final BalanceHistoryRepository balanceHistoryRepository;

    public JpaUserBalanceRepository(UserRepository userRepository,
                                   BalanceHistoryRepository balanceHistoryRepository) {
        this.userRepository = userRepository;
        this.balanceHistoryRepository = balanceHistoryRepository;
    }

    @Override
    public User save(User user) {
        return userRepository.save(user);
    }

    @Override
    public Optional<User> findById(Long userId) {
        return userRepository.findById(userId);
    }

    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.userId = :userId")
    public Optional<User> findByIdWithLock(@Param("userId") Long userId) {
        return userRepository.findById(userId);
    }

    @Override
    public BalanceHistory save(BalanceHistory balanceHistory) {
        return balanceHistoryRepository.save(balanceHistory);
    }

    @Override
    public List<BalanceHistory> findByUserIdOrderByCreatedAtDesc(Long userId) {
        return balanceHistoryRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    @Transactional
    public void chargeBalance(Long userId, BigDecimal amount) {
        User user = findByIdWithLock(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.chargeBalance(amount);
        save(user);

        // 잔액 충전 이력 저장
        BalanceHistory history = new BalanceHistory(userId, "CHARGE", amount, user.getBalance());
        save(history);
    }

    @Override
    @Transactional
    public void deductBalance(Long userId, BigDecimal amount) {
        User user = findByIdWithLock(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (user.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient balance");
        }

        user.deductBalance(amount);
        save(user);

        // 잔액 차감 이력 저장
        BalanceHistory history = new BalanceHistory(userId, "DEDUCT", amount, user.getBalance());
        save(history);
    }

    @Override
    public BigDecimal getBalance(Long userId) {
        return findById(userId)
                .map(User::getBalance)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }
}