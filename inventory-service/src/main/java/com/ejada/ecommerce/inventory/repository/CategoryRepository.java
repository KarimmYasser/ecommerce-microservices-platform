package com.ejada.ecommerce.inventory.repository;

import com.ejada.ecommerce.inventory.domain.Category;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {

	boolean existsBySlug(String slug);

	Optional<Category> findBySlug(String slug);

}
