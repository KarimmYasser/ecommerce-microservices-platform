package com.ejada.ecommerce.shop.service.impl;

import com.ejada.ecommerce.shop.service.CartService;

import com.ejada.ecommerce.shop.client.ResilientInventoryClient;
import com.ejada.ecommerce.shop.client.dto.ProductBatchItem;
import com.ejada.ecommerce.shop.domain.Cart;
import com.ejada.ecommerce.shop.domain.CartItem;
import com.ejada.ecommerce.shop.dto.CartItemInput;
import com.ejada.ecommerce.shop.dto.CartItemQuantityUpdate;
import com.ejada.ecommerce.shop.dto.CartResponse;
import com.ejada.ecommerce.shop.exception.ResourceNotFoundException;
import com.ejada.ecommerce.shop.mapper.CartMapper;
import com.ejada.ecommerce.shop.repository.CartItemRepository;
import com.ejada.ecommerce.shop.repository.CartRepository;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class CartServiceImpl implements CartService {

	private final CartRepository cartRepository;

	private final CartItemRepository cartItemRepository;

	private final ResilientInventoryClient inventoryClient;

	private final CartMapper cartMapper;

	@Transactional
	@Override
	public CartResponse getCart(Long userId) {
		Cart cart = getOrCreateCart(userId);
		Map<Long, ProductBatchItem> liveProducts = fetchLiveProducts(cart);
		return cartMapper.toCartResponse(cart, liveProducts);
	}

	@Transactional
	@Override
	public CartResponse addItem(Long userId, CartItemInput input) {
		Cart cart = getOrCreateCart(userId);
		Optional<CartItem> existingItem = cartItemRepository.findByCartIdAndProductIdAndVariantId(
				cart.getId(), input.productId(), input.variantId());

		if (existingItem.isPresent()) {
			CartItem item = existingItem.get();
			item.setQuantity(item.getQuantity() + input.quantity());
			cartItemRepository.save(item);
		} else {
			BigDecimal priceSnapshot = fetchPriceSnapshot(input.productId());
			CartItem newItem = CartItem.builder()
					.cart(cart)
					.productId(input.productId())
					.variantId(input.variantId())
					.quantity(input.quantity())
					.unitPriceSnapshot(priceSnapshot)
					.build();
			cart.addItem(newItem);
			cartItemRepository.save(newItem);
		}

		Cart updatedCart = cartRepository.save(cart);
		Map<Long, ProductBatchItem> liveProducts = fetchLiveProducts(updatedCart);
		return cartMapper.toCartResponse(updatedCart, liveProducts);
	}

	@Transactional
	@Override
	public CartResponse updateItemQuantity(Long userId, Long itemId, CartItemQuantityUpdate update) {
		Cart cart = getOrCreateCart(userId);
		CartItem item = cartItemRepository.findByIdAndCartId(itemId, cart.getId())
				.orElseThrow(() -> new ResourceNotFoundException("Cart item not found: " + itemId));

		item.setQuantity(update.quantity());
		cartItemRepository.save(item);

		Map<Long, ProductBatchItem> liveProducts = fetchLiveProducts(cart);
		return cartMapper.toCartResponse(cart, liveProducts);
	}

	@Transactional
	@Override
	public CartResponse removeItem(Long userId, Long itemId) {
		Cart cart = getOrCreateCart(userId);
		CartItem item = cartItemRepository.findByIdAndCartId(itemId, cart.getId())
				.orElseThrow(() -> new ResourceNotFoundException("Cart item not found: " + itemId));

		cart.removeItem(item);
		cartItemRepository.delete(item);

		Map<Long, ProductBatchItem> liveProducts = fetchLiveProducts(cart);
		return cartMapper.toCartResponse(cart, liveProducts);
	}

	@Transactional
	@Override
	public void clearCart(Long userId) {
		cartRepository.findByUserId(userId).ifPresent(cart -> {
			cart.getItems().clear();
			cartRepository.save(cart);
		});
	}

	private Cart getOrCreateCart(Long userId) {
		return cartRepository.findByUserId(userId)
				.orElseGet(() -> cartRepository.save(Cart.builder().userId(userId).build()));
	}

	private Map<Long, ProductBatchItem> fetchLiveProducts(Cart cart) {
		List<Long> productIds = cart.getItems().stream()
				.map(CartItem::getProductId)
				.distinct()
				.toList();

		if (productIds.isEmpty()) {
			return Collections.emptyMap();
		}

		try {
			List<ProductBatchItem> items = inventoryClient.getProductsBatch(productIds);
			if (items == null) {
				return Collections.emptyMap();
			}
			return items.stream().collect(Collectors.toMap(ProductBatchItem::id, Function.identity(), (a, b) -> a));
		} catch (Exception ex) {
			return Collections.emptyMap();
		}
	}

	private BigDecimal fetchPriceSnapshot(Long productId) {
		try {
			List<ProductBatchItem> items = inventoryClient.getProductsBatch(List.of(productId));
			if (items != null && !items.isEmpty()) {
				return items.get(0).basePrice();
			}
		} catch (Exception ignored) {
		}
		return BigDecimal.ZERO;
	}

}
