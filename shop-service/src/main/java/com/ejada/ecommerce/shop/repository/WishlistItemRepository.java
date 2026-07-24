package com.ejada.ecommerce.shop.repository;

import com.ejada.ecommerce.shop.domain.WishlistItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WishlistItemRepository extends JpaRepository<WishlistItem, Long> {

	List<WishlistItem> findByUserId(Long userId);

	Optional<WishlistItem> findByUserIdAndProductId(Long userId, Long productId);

	boolean existsByUserIdAndProductId(Long userId, Long productId);

}
