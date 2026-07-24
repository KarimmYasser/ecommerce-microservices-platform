package com.ejada.ecommerce.shop.service.impl;

import com.ejada.ecommerce.shop.service.ReviewService;

import com.ejada.ecommerce.shop.domain.Review;
import com.ejada.ecommerce.shop.dto.PageResponse;
import com.ejada.ecommerce.shop.dto.ReviewCreateRequest;
import com.ejada.ecommerce.shop.dto.ReviewResponse;
import com.ejada.ecommerce.shop.exception.DuplicateResourceException;
import com.ejada.ecommerce.shop.exception.ResourceNotFoundException;
import com.ejada.ecommerce.shop.mapper.ReviewMapper;
import com.ejada.ecommerce.shop.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class ReviewServiceImpl implements ReviewService {

	private final ReviewRepository reviewRepository;

	private final ReviewMapper reviewMapper;

	@Transactional(readOnly = true)
	@Override
	public PageResponse<ReviewResponse> getProductReviews(Long productId, Pageable pageable) {
		Page<ReviewResponse> page = reviewRepository.findByProductId(productId, pageable)
				.map(reviewMapper::toResponse);
		return PageResponse.of(page);
	}

	@Transactional
	@Override
	public ReviewResponse createReview(Long userId, String authorName, Long productId, ReviewCreateRequest request) {
		if (reviewRepository.existsByProductIdAndUserId(productId, userId)) {
			throw new DuplicateResourceException("User has already reviewed this product");
		}

		String displayName = (authorName != null && !authorName.isBlank()) ? authorName : "Customer";

		Review review = Review.builder()
				.productId(productId)
				.userId(userId)
				.authorNameSnapshot(displayName)
				.rating(request.rating())
				.title(request.title())
				.body(request.body())
				.build();

		Review savedReview = reviewRepository.save(review);
		return reviewMapper.toResponse(savedReview);
	}

	@Transactional
	@Override
	public void deleteReview(Long userId, Long reviewId) {
		Review review = reviewRepository.findByIdAndUserId(reviewId, userId)
				.orElseThrow(() -> new ResourceNotFoundException("Review not found: " + reviewId));
		reviewRepository.delete(review);
	}

}
