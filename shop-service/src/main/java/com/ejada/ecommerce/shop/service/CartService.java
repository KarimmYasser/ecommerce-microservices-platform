package com.ejada.ecommerce.shop.service;

import com.ejada.ecommerce.shop.dto.CartItemInput;
import com.ejada.ecommerce.shop.dto.CartItemQuantityUpdate;
import com.ejada.ecommerce.shop.dto.CartResponse;

public interface CartService {

	CartResponse getCart(Long userId);

	CartResponse addItem(Long userId, CartItemInput input);

	CartResponse updateItemQuantity(Long userId, Long itemId, CartItemQuantityUpdate update);

	CartResponse removeItem(Long userId, Long itemId);

	void clearCart(Long userId);

}
