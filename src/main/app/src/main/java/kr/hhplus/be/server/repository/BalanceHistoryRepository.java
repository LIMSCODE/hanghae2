package kr.hhplus.be.server.repository;

import kr.hhplus.be.server.domain.BalanceHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BalanceHistoryRepository extends JpaRepository<BalanceHistory, Long> {

    Page<BalanceHistory> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}