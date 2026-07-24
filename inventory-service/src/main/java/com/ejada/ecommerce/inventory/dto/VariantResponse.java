package com.ejada.ecommerce.inventory.dto;

import java.math.BigDecimal;

public record VariantResponse(
		Long id,
		String sku,
		String size,
		String color,
		BigDecimal price,
		int available) {
}
