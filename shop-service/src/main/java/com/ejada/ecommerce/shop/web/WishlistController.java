package com.ejada.ecommerce.shop.web;

import com.ejada.ecommerce.shop.dto.WishlistItemInput;
import com.ejada.ecommerce.shop.dto.WishlistItemResponse;
import com.ejada.ecommerce.shop.service.WishlistService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/wishlist")
public class WishlistController {

	private final WishlistService wishlistService;

	@GetMapping
	public ResponseEntity<List<WishlistItemResponse>> getWishlist(Principal principal) {
		Long userId = getUserId(principal);
		return ResponseEntity.ok(wishlistService.getWishlist(userId));
	}

	@PostMapping("/items")
	public ResponseEntity<WishlistItemResponse> addItem(Principal principal, @Valid @RequestBody WishlistItemInput input) {
		Long userId = getUserId(principal);
		return ResponseEntity.status(HttpStatus.CREATED).body(wishlistService.addItem(userId, input));
	}

	@DeleteMapping("/items/{productId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void removeItem(Principal principal, @PathVariable("productId") Long productId) {
		Long userId = getUserId(principal);
		wishlistService.removeItem(userId, productId);
	}

	private Long getUserId(Principal principal) {
		if (principal == null || principal.getName() == null) {
			throw new IllegalStateException("Unauthenticated request");
		}
		return Long.parseLong(principal.getName());
	}

}
