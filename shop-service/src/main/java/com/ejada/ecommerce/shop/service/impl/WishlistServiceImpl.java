package com.ejada.ecommerce.shop.service.impl;

import com.ejada.ecommerce.shop.service.WishlistService;

import com.ejada.ecommerce.shop.client.InventoryClient;
import com.ejada.ecommerce.shop.client.dto.ProductBatchItem;
import com.ejada.ecommerce.shop.domain.WishlistItem;
import com.ejada.ecommerce.shop.dto.WishlistItemInput;
import com.ejada.ecommerce.shop.dto.WishlistItemResponse;
import com.ejada.ecommerce.shop.exception.ResourceNotFoundException;
import com.ejada.ecommerce.shop.mapper.WishlistMapper;
import com.ejada.ecommerce.shop.repository.WishlistItemRepository;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class WishlistServiceImpl implements WishlistService {

	private final WishlistItemRepository wishlistItemRepository;

	private final InventoryClient inventoryClient;

	private final WishlistMapper wishlistMapper;

	@Transactional(readOnly = true)
	@Override
	public List<WishlistItemResponse> getWishlist(Long userId) {
		List<WishlistItem> items = wishlistItemRepository.findByUserId(userId);
		if (items.isEmpty()) {
			return List.of();
		}
		Map<Long, ProductBatchItem> liveProducts = fetchLiveProducts(items);
		return items.stream()
				.map(item -> wishlistMapper.toResponse(item, liveProducts.get(item.getProductId())))
				.toList();
	}

	@Transactional
	@Override
	public WishlistItemResponse addItem(Long userId, WishlistItemInput input) {
		WishlistItem item = wishlistItemRepository.findByUserIdAndProductId(userId, input.productId())
				.orElseGet(() -> wishlistItemRepository.save(
						WishlistItem.builder()
								.userId(userId)
								.productId(input.productId())
								.build()));

		Map<Long, ProductBatchItem> liveProducts = fetchLiveProducts(List.of(item));
		return wishlistMapper.toResponse(item, liveProducts.get(input.productId()));
	}

	@Transactional
	@Override
	public void removeItem(Long userId, Long productId) {
		WishlistItem item = wishlistItemRepository.findByUserIdAndProductId(userId, productId)
				.orElseThrow(() -> new ResourceNotFoundException("Wishlist item not found for product: " + productId));
		wishlistItemRepository.delete(item);
	}

	private Map<Long, ProductBatchItem> fetchLiveProducts(List<WishlistItem> items) {
		List<Long> productIds = items.stream()
				.map(WishlistItem::getProductId)
				.distinct()
				.toList();

		if (productIds.isEmpty()) {
			return Collections.emptyMap();
		}

		try {
			List<ProductBatchItem> batch = inventoryClient.getProductsBatch(productIds);
			if (batch == null) {
				return Collections.emptyMap();
			}
			return batch.stream().collect(Collectors.toMap(ProductBatchItem::id, Function.identity(), (a, b) -> a));
		} catch (Exception ex) {
			return Collections.emptyMap();
		}
	}

}
