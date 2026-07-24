package com.ejada.ecommerce.shop.service;

import com.ejada.ecommerce.shop.dto.WishlistItemInput;
import com.ejada.ecommerce.shop.dto.WishlistItemResponse;
import java.util.List;

public interface WishlistService {

	List<WishlistItemResponse> getWishlist(Long userId);

	WishlistItemResponse addItem(Long userId, WishlistItemInput input);

	void removeItem(Long userId, Long productId);

}
