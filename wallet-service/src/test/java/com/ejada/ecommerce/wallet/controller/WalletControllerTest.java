package com.ejada.ecommerce.wallet.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ejada.ecommerce.wallet.config.JwtService;
import com.ejada.ecommerce.wallet.dto.AmountRequest;
import com.ejada.ecommerce.wallet.dto.PageResponse;
import com.ejada.ecommerce.wallet.dto.WalletMutationResponse;
import com.ejada.ecommerce.wallet.dto.WalletResponse;
import com.ejada.ecommerce.wallet.exception.InsufficientFundsException;
import com.ejada.ecommerce.wallet.service.WalletService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@Import({ JwtService.class, com.ejada.ecommerce.wallet.config.SecurityConfig.class })
@WebMvcTest(WalletController.class)
class WalletControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private WalletService walletService;

	@Test
	void getMyWallet_withoutToken_returns401() throws Exception {
		mockMvc.perform(get("/api/v1/wallets/me")).andExpect(status().isUnauthorized());
	}

	@Test
	@WithMockUser(username = "5")
	void getMyWallet_withToken_returns200() throws Exception {
		when(walletService.getWallet(5L)).thenReturn(new WalletResponse(new BigDecimal("100.00"), "USD"));

		mockMvc.perform(get("/api/v1/wallets/me")).andExpect(status().isOk());
	}

	@Test
	@WithMockUser(username = "5")
	void deposit_withValidAmount_returns200() throws Exception {
		when(walletService.deposit(eq(5L), any())).thenReturn(new WalletMutationResponse(new BigDecimal("150.00"), 1L));

		mockMvc.perform(post("/api/v1/wallets/me/deposit").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(new AmountRequest(new BigDecimal("50.00")))))
				.andExpect(status().isOk());
	}

	@Test
	@WithMockUser(username = "5")
	void deposit_withNegativeAmount_returns400() throws Exception {
		mockMvc.perform(post("/api/v1/wallets/me/deposit").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(new AmountRequest(new BigDecimal("-50.00")))))
				.andExpect(status().isBadRequest());
	}

	@Test
	@WithMockUser(username = "5")
	void withdraw_whenInsufficientFunds_returns402() throws Exception {
		when(walletService.withdraw(eq(5L), any())).thenThrow(new InsufficientFundsException());

		mockMvc.perform(post("/api/v1/wallets/me/withdraw").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(new AmountRequest(new BigDecimal("999999.00")))))
				.andExpect(status().isPaymentRequired());
	}

	@Test
	@WithMockUser(username = "5")
	void getTransactions_returns200() throws Exception {
		when(walletService.getTransactions(eq(5L), org.mockito.ArgumentMatchers.any(Pageable.class)))
				.thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0));

		mockMvc.perform(get("/api/v1/wallets/me/transactions")).andExpect(status().isOk());
	}

}
