package com.ejada.ecommerce.inventory.repository.spec;

import java.math.BigDecimal;
import org.springframework.data.jpa.domain.Specification;

import com.ejada.ecommerce.inventory.domain.Product;

/**
 * Composable filter predicates for {@code GET /products}. Combined in
 * ProductServiceImpl via {@code Specification.allOf(...)} — each method
 * returns {@code null} when its criterion is absent so it drops out of the
 * conjunction cleanly.
 */
public final class ProductSpecifications {

	private ProductSpecifications() {
	}

	public static Specification<Product> isActive() {
		return (root, query, cb) -> cb.isTrue(root.get("isActive"));
	}

	public static Specification<Product> nameOrDescriptionContains(String q) {
		if (q == null || q.isBlank()) {
			return null;
		}
		String like = "%" + q.toLowerCase() + "%";
		return (root, query, cb) -> cb.or(
				cb.like(cb.lower(root.get("name")), like),
				cb.like(cb.lower(root.get("description")), like));
	}

	public static Specification<Product> hasCategoryId(Long categoryId) {
		if (categoryId == null) {
			return null;
		}
		return (root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId);
	}

	public static Specification<Product> isNew(Boolean isNew) {
		if (isNew == null) {
			return null;
		}
		return (root, query, cb) -> cb.equal(root.get("isNew"), isNew);
	}

	public static Specification<Product> onSale(Boolean onSale) {
		if (onSale == null || !onSale) {
			return null;
		}
		return (root, query, cb) -> cb.isNotNull(root.get("compareAtPrice"));
	}

	public static Specification<Product> minPrice(BigDecimal minPrice) {
		if (minPrice == null) {
			return null;
		}
		return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("basePrice"), minPrice);
	}

	public static Specification<Product> maxPrice(BigDecimal maxPrice) {
		if (maxPrice == null) {
			return null;
		}
		return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("basePrice"), maxPrice);
	}

	/** Drops {@code null} specs (absent filters) before AND-combining the rest. */
	@SafeVarargs
	public static Specification<Product> allOf(Specification<Product>... specs) {
		Specification<Product> combined = (root, query, cb) -> cb.conjunction();
		for (Specification<Product> spec : specs) {
			if (spec != null) {
				combined = combined.and(spec);
			}
		}
		return combined;
	}

}
