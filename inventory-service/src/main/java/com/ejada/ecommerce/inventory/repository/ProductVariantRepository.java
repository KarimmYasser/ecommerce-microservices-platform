package com.ejada.ecommerce.inventory.repository;

import com.ejada.ecommerce.inventory.domain.ProductVariant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

	boolean existsBySku(String sku);

	List<ProductVariant> findByIdIn(List<Long> ids);

	Optional<ProductVariant> findByIdAndProductId(Long id, Long productId);

}
