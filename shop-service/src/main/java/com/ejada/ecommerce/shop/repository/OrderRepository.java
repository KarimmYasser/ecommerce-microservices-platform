package com.ejada.ecommerce.shop.repository;

import com.ejada.ecommerce.shop.domain.Order;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {

	Optional<Order> findByIdAndUserId(Long id, Long userId);

	Page<Order> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

	boolean existsByOrderNumber(String orderNumber);

}
