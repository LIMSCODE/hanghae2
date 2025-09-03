package kr.hhplus.be.server.repository;

import kr.hhplus.be.server.domain.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Page<Order> findByUserIdOrderByOrderedAtDesc(Long userId, Pageable pageable);

    @Query("SELECT o FROM Order o JOIN FETCH o.orderItems WHERE o.orderId = :orderId")
    Order findByIdWithItems(@Param("orderId") Long orderId);

    List<Order> findByOrderStatusAndOrderedAtBetween(Order.OrderStatus orderStatus, 
                                                    LocalDateTime startDate, 
                                                    LocalDateTime endDate);
}