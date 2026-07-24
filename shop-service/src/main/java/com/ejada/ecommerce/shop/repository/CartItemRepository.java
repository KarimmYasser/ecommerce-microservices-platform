package com.ejada.ecommerce.shop.repository;

import com.ejada.ecommerce.shop.domain.CartItem;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

	Optional<CartItem> findByIdAndCartId(Long id, Long cartId);

	Optional<CartItem> findByCartIdAndProductIdAndVariantId(Long cartId, Long productId, Long variantId);

}
