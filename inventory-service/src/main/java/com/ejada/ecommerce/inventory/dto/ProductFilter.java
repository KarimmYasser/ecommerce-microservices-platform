package com.ejada.ecommerce.inventory.dto;

import java.math.BigDecimal;

/** Search/filter criteria for {@code GET /products} — see ProductSpecifications. */
public record ProductFilter(
		String q,
		Long categoryId,
		Boolean isNew,
		Boolean onSale,
		BigDecimal minPrice,
		BigDecimal maxPrice) {
}
