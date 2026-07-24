package com.ejada.ecommerce.shop.dto;

import java.math.BigDecimal;

public record CartLineResponse(
		Long id,
		Long productId,
		Long variantId,
		String name,
		String primaryImageUrl,
		int quantity,
		BigDecimal unitPrice,
		BigDecimal lineTotal,
		boolean priceUnconfirmed) {
}
