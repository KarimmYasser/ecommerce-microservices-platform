package com.ejada.ecommerce.inventory.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

import com.ejada.ecommerce.inventory.domain.Category;

public interface CategoryRepository extends JpaRepository<Category, Long> {

	boolean existsBySlug(String slug);

	Optional<Category> findBySlug(String slug);

}
