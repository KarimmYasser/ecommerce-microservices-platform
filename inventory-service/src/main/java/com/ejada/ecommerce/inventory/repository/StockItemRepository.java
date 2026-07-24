package com.ejada.ecommerce.inventory.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import com.ejada.ecommerce.inventory.domain.StockItem;

import jakarta.persistence.LockModeType;

public interface StockItemRepository extends JpaRepository<StockItem, Long> {

	Optional<StockItem> findByVariantId(Long variantId);

	List<StockItem> findByVariantIdIn(List<Long> variantIds);

	/**
	 * Pessimistic write lock so concurrent reserve/release calls for the same
	 * variant serialize at the database instead of racing on the in-memory
	 * {@code @Version} check-then-write and retrying.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select s from StockItem s where s.variant.id = :variantId")
	Optional<StockItem> findByVariantIdForUpdate(Long variantId);

}
