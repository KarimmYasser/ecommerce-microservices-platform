package com.ejada.ecommerce.shop.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ejada.ecommerce.shop.config.JwtService;
import com.ejada.ecommerce.shop.config.SecurityConfig;
import com.ejada.ecommerce.shop.domain.OrderStatus;
import com.ejada.ecommerce.shop.dto.OrderResponse;
import com.ejada.ecommerce.shop.exception.InsufficientStockException;
import com.ejada.ecommerce.shop.exception.PaymentFailedException;
import com.ejada.ecommerce.shop.service.OrderService;
import java.math.BigDecimal;
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
@WebMvcTest(OrderController.class)
class OrderControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private OrderService orderService;

	@Test
	void checkout_unauthenticated_returns401() throws Exception {
		mockMvc.perform(post("/api/v1/orders")).andExpect(status().isUnauthorized());
	}

	@Test
	@WithMockUser(username = "1")
	void checkout_success_returns201() throws Exception {
		OrderResponse response = new OrderResponse(
				100L, "ORD-123", OrderStatus.CONFIRMED, null,
				new BigDecimal("100.00"), BigDecimal.ZERO, new BigDecimal("100.00"),
				"USD", "999", List.of(), Instant.now());

		when(orderService.checkout(eq(1L), any())).thenReturn(response);

		mockMvc.perform(post("/api/v1/orders"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.orderNumber").value("ORD-123"))
				.andExpect(jsonPath("$.status").value("CONFIRMED"));
	}

	@Test
	@WithMockUser(username = "1")
	void checkout_stockShortfall_returns409() throws Exception {
		when(orderService.checkout(eq(1L), any())).thenThrow(new InsufficientStockException("Out of stock"));

		mockMvc.perform(post("/api/v1/orders"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error").value("OUT_OF_STOCK"));
	}

	@Test
	@WithMockUser(username = "1")
	void checkout_insufficientFunds_returns402() throws Exception {
		when(orderService.checkout(eq(1L), any())).thenThrow(new PaymentFailedException("Payment failed"));

		mockMvc.perform(post("/api/v1/orders"))
				.andExpect(status().isPaymentRequired())
				.andExpect(jsonPath("$.error").value("PAYMENT_FAILED"));
	}

	@Test
	@WithMockUser(username = "1")
	void cancelOrder_success_returns200() throws Exception {
		OrderResponse response = new OrderResponse(
				100L, "ORD-123", OrderStatus.CANCELLED, null,
				new BigDecimal("100.00"), BigDecimal.ZERO, new BigDecimal("100.00"),
				"USD", "999", List.of(), Instant.now());

		when(orderService.cancelOrder(1L, 100L)).thenReturn(response);

		mockMvc.perform(post("/api/v1/orders/100/cancel"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CANCELLED"));
	}

}
