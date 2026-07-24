package com.ejada.ecommerce.shop.web;

import com.ejada.ecommerce.shop.dto.PageResponse;
import com.ejada.ecommerce.shop.dto.ReviewCreateRequest;
import com.ejada.ecommerce.shop.dto.ReviewResponse;
import com.ejada.ecommerce.shop.service.ReviewService;
import jakarta.validation.Valid;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
public class ReviewController {

	private final ReviewService reviewService;

	@GetMapping("/api/v1/products/{productId}/reviews")
	public ResponseEntity<PageResponse<ReviewResponse>> getProductReviews(
			@PathVariable("productId") Long productId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size) {
		Pageable pageable = PageRequest.of(page, size);
		return ResponseEntity.ok(reviewService.getProductReviews(productId, pageable));
	}

	@PostMapping("/api/v1/products/{productId}/reviews")
	public ResponseEntity<ReviewResponse> createReview(
			Principal principal,
			@PathVariable("productId") Long productId,
			@Valid @RequestBody ReviewCreateRequest request) {
		Long userId = getUserId(principal);
		// The JWT only carries userId + roles, no display name, so let the service's
		// fallback ("Customer") apply rather than showing the raw numeric userId.
		ReviewResponse response = reviewService.createReview(userId, null, productId, request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@DeleteMapping("/api/v1/reviews/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteReview(Principal principal, @PathVariable("id") Long id) {
		Long userId = getUserId(principal);
		reviewService.deleteReview(userId, id);
	}

	private Long getUserId(Principal principal) {
		if (principal == null || principal.getName() == null) {
			throw new IllegalStateException("Unauthenticated request");
		}
		return Long.parseLong(principal.getName());
	}

}
