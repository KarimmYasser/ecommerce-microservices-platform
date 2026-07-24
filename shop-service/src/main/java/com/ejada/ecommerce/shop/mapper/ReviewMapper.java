package com.ejada.ecommerce.shop.mapper;

import com.ejada.ecommerce.shop.domain.Review;
import com.ejada.ecommerce.shop.dto.ReviewResponse;
import org.springframework.stereotype.Component;

@Component
public class ReviewMapper {

	public ReviewResponse toResponse(Review review) {
		return new ReviewResponse(
				review.getId(),
				review.getProductId(),
				review.getAuthorNameSnapshot(),
				review.getRating(),
				review.getTitle(),
				review.getBody(),
				review.getCreatedAt());
	}

}
