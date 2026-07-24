package com.ejada.ecommerce.shop.mapper;

import com.ejada.ecommerce.shop.client.dto.ProductBatchItem;
import com.ejada.ecommerce.shop.domain.WishlistItem;
import com.ejada.ecommerce.shop.dto.WishlistItemResponse;
import org.springframework.stereotype.Component;

@Component
public class WishlistMapper {

	/** {@code liveProduct} is null when inventory didn't return data for this product. */
	public WishlistItemResponse toResponse(WishlistItem item, ProductBatchItem liveProduct) {
		if (liveProduct == null) {
			return new WishlistItemResponse(item.getProductId(), null, null, null, null);
		}
		return new WishlistItemResponse(
				item.getProductId(),
				liveProduct.name(),
				liveProduct.basePrice(),
				liveProduct.currency(),
				liveProduct.primaryImageUrl());
	}

}
