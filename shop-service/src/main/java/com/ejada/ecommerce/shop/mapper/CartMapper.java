package com.ejada.ecommerce.shop.mapper;

import com.ejada.ecommerce.shop.client.dto.ProductBatchItem;
import com.ejada.ecommerce.shop.domain.Cart;
import com.ejada.ecommerce.shop.domain.CartItem;
import com.ejada.ecommerce.shop.dto.CartLineResponse;
import com.ejada.ecommerce.shop.dto.CartResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class CartMapper {

	/**
	 * @param liveProducts keyed by productId; a missing entry means inventory
	 *                     didn't return live data for that line (fallback path
	 *                     — see docs/api/inter-service-feign.md), so the line
	 *                     falls back to its snapshot price flagged unconfirmed.
	 */
	public CartResponse toCartResponse(Cart cart, Map<Long, ProductBatchItem> liveProducts) {
		List<CartLineResponse> lines = cart.getItems().stream()
				.map(item -> toLineResponse(item, liveProducts.get(item.getProductId())))
				.toList();
		BigDecimal subtotal = lines.stream().map(CartLineResponse::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
		return new CartResponse(cart.getId(), lines, subtotal);
	}

	private CartLineResponse toLineResponse(CartItem item, ProductBatchItem liveProduct) {
		boolean unconfirmed = liveProduct == null;
		BigDecimal unitPrice = unconfirmed ? item.getUnitPriceSnapshot() : liveProduct.basePrice();
		return new CartLineResponse(
				item.getId(),
				item.getProductId(),
				item.getVariantId(),
				unconfirmed ? null : liveProduct.name(),
				unconfirmed ? null : liveProduct.primaryImageUrl(),
				item.getQuantity(),
				unitPrice,
				unitPrice.multiply(BigDecimal.valueOf(item.getQuantity())),
				unconfirmed);
	}

}
