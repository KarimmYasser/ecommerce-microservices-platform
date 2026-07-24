package com.ejada.ecommerce.shop.dto;

import java.math.BigDecimal;

public record OrderItemResponse(
		Long productId,
		Long variantId,
		String productNameSnapshot,
		BigDecimal unitPrice,
		int quantity,
		BigDecimal lineTotal) {
}
