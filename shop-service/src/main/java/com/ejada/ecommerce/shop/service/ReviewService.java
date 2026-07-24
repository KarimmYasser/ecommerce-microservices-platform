package com.ejada.ecommerce.shop.service;

import com.ejada.ecommerce.shop.dto.PageResponse;
import com.ejada.ecommerce.shop.dto.ReviewCreateRequest;
import com.ejada.ecommerce.shop.dto.ReviewResponse;
import org.springframework.data.domain.Pageable;

public interface ReviewService {

	PageResponse<ReviewResponse> getProductReviews(Long productId, Pageable pageable);

	ReviewResponse createReview(Long userId, String authorName, Long productId, ReviewCreateRequest request);

	void deleteReview(Long userId, Long reviewId);

}
