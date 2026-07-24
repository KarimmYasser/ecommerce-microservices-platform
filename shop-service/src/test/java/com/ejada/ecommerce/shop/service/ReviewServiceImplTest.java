package com.ejada.ecommerce.shop.service;

import static com.ejada.ecommerce.shop.support.EntityTestSupport.withId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ejada.ecommerce.shop.domain.Review;
import com.ejada.ecommerce.shop.dto.ReviewCreateRequest;
import com.ejada.ecommerce.shop.dto.ReviewResponse;
import com.ejada.ecommerce.shop.service.impl.ReviewServiceImpl;
import com.ejada.ecommerce.shop.exception.DuplicateResourceException;
import com.ejada.ecommerce.shop.mapper.ReviewMapper;
import com.ejada.ecommerce.shop.repository.ReviewRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {

	@Mock
	private ReviewRepository reviewRepository;

	private ReviewMapper reviewMapper = new ReviewMapper();

	private ReviewServiceImpl reviewService;

	private final Long userId = 1L;

	@BeforeEach
	void setUp() {
		reviewService = new ReviewServiceImpl(reviewRepository, reviewMapper);
	}

	@Test
	void createReview_success() {
		when(reviewRepository.existsByProductIdAndUserId(10L, userId)).thenReturn(false);
		when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> withId(inv.getArgument(0), 100L));

		ReviewCreateRequest request = new ReviewCreateRequest(5, "Great Product", "Loved it!");
		ReviewResponse response = reviewService.createReview(userId, "John Doe", 10L, request);

		assertThat(response).isNotNull();
		assertThat(response.rating()).isEqualTo(5);
		assertThat(response.title()).isEqualTo("Great Product");
		assertThat(response.authorNameSnapshot()).isEqualTo("John Doe");
	}

	@Test
	void createReview_duplicateThrowsConflict() {
		when(reviewRepository.existsByProductIdAndUserId(10L, userId)).thenReturn(true);

		ReviewCreateRequest request = new ReviewCreateRequest(5, "Great Product", "Loved it!");
		assertThatThrownBy(() -> reviewService.createReview(userId, "John Doe", 10L, request))
				.isInstanceOf(DuplicateResourceException.class);
	}

	@Test
	void deleteReview_success() {
		Review review = withId(Review.builder().userId(userId).productId(10L).build(), 100L);

		when(reviewRepository.findByIdAndUserId(100L, userId)).thenReturn(Optional.of(review));

		reviewService.deleteReview(userId, 100L);

		verify(reviewRepository).delete(review);
	}

}
