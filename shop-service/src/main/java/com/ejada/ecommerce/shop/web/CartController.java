package com.ejada.ecommerce.shop.web;

import com.ejada.ecommerce.shop.dto.CartItemInput;
import com.ejada.ecommerce.shop.dto.CartItemQuantityUpdate;
import com.ejada.ecommerce.shop.dto.CartResponse;
import com.ejada.ecommerce.shop.service.CartService;
import jakarta.validation.Valid;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/cart")
public class CartController {

	private final CartService cartService;

	@GetMapping
	public ResponseEntity<CartResponse> getCart(Principal principal) {
		Long userId = getUserId(principal);
		return ResponseEntity.ok(cartService.getCart(userId));
	}

	@PostMapping("/items")
	public ResponseEntity<CartResponse> addItem(Principal principal, @Valid @RequestBody CartItemInput input) {
		Long userId = getUserId(principal);
		return ResponseEntity.ok(cartService.addItem(userId, input));
	}

	@PutMapping("/items/{itemId}")
	public ResponseEntity<CartResponse> updateItemQuantity(
			Principal principal,
			@PathVariable("itemId") Long itemId,
			@Valid @RequestBody CartItemQuantityUpdate update) {
		Long userId = getUserId(principal);
		return ResponseEntity.ok(cartService.updateItemQuantity(userId, itemId, update));
	}

	@DeleteMapping("/items/{itemId}")
	public ResponseEntity<CartResponse> removeItem(Principal principal, @PathVariable("itemId") Long itemId) {
		Long userId = getUserId(principal);
		return ResponseEntity.ok(cartService.removeItem(userId, itemId));
	}

	@DeleteMapping
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void clearCart(Principal principal) {
		Long userId = getUserId(principal);
		cartService.clearCart(userId);
	}

	private Long getUserId(Principal principal) {
		if (principal == null || principal.getName() == null) {
			throw new IllegalStateException("Unauthenticated request");
		}
		return Long.parseLong(principal.getName());
	}

}
