package com.ejada.ecommerce.shop.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ejada.ecommerce.shop.config.JwtService;
import com.ejada.ecommerce.shop.config.SecurityConfig;
import com.ejada.ecommerce.shop.dto.PageResponse;
import com.ejada.ecommerce.shop.dto.ReviewCreateRequest;
import com.ejada.ecommerce.shop.dto.ReviewResponse;
import com.ejada.ecommerce.shop.service.ReviewService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@Import({ JwtService.class, SecurityConfig.class })
@WebMvcTest(ReviewController.class)
class ReviewControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ReviewService reviewService;

	@Test
	void getProductReviews_isPublic_returns200() throws Exception {
		ReviewResponse review = new ReviewResponse(100L, 10L, "John", 5, "Great", "Body", Instant.now());
		PageResponse<ReviewResponse> page = new PageResponse<>(List.of(review), 0, 10, 1, 1);
		when(reviewService.getProductReviews(eq(10L), any())).thenReturn(page);

		mockMvc.perform(get("/api/v1/products/10/reviews"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].authorNameSnapshot").value("John"));
	}

	@Test
	@WithMockUser(username = "1")
	void createReview_valid_returns201() throws Exception {
		ReviewResponse review = new ReviewResponse(100L, 10L, "John", 5, "Great", "Body", Instant.now());
		when(reviewService.createReview(eq(1L), any(), eq(10L), any(ReviewCreateRequest.class))).thenReturn(review);

		mockMvc.perform(post("/api/v1/products/10/reviews")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"rating\":5,\"title\":\"Great\",\"body\":\"Loved it!\"}"))
				.andExpect(status().isCreated());
	}

	@Test
	@WithMockUser(username = "1")
	void deleteReview_returns204() throws Exception {
		mockMvc.perform(delete("/api/v1/reviews/100"))
				.andExpect(status().isNoContent());
	}

}
