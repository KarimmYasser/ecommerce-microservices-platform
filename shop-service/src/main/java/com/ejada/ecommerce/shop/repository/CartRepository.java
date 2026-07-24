package com.ejada.ecommerce.shop.repository;

import com.ejada.ecommerce.shop.domain.Cart;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartRepository extends JpaRepository<Cart, Long> {

	Optional<Cart> findByUserId(Long userId);

}
