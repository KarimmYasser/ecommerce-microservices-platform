package com.ejada.ecommerce.wallet.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ejada.ecommerce.wallet.config.JwtService;
import com.ejada.ecommerce.wallet.dto.BalanceResponse;
import com.ejada.ecommerce.wallet.dto.DebitResponse;
import com.ejada.ecommerce.wallet.exception.InsufficientFundsException;
import com.ejada.ecommerce.wallet.service.WalletService;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Internal endpoints are unauthenticated at this layer (protected by network
 * topology instead — see SecurityConfig / docs/infrastructure/api-gateway.md).
 */
@Import({ JwtService.class, com.ejada.ecommerce.wallet.config.SecurityConfig.class })
@WebMvcTest(WalletInternalController.class)
class WalletInternalControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private WalletService walletService;

	@Test
	void debit_withoutToken_returns200() throws Exception {
		when(walletService.debit(eq(5L), any())).thenReturn(new DebitResponse(1L, new BigDecimal("50.00")));

		mockMvc.perform(post("/wallets/5/debit").contentType(MediaType.APPLICATION_JSON)
				.content("{\"amount\":50.00,\"currency\":\"USD\",\"idempotencyKey\":\"order-1\"}"))
				.andExpect(status().isOk());
	}

	@Test
	void debit_whenInsufficientFunds_returns402() throws Exception {
		when(walletService.debit(eq(5L), any())).thenThrow(new InsufficientFundsException());

		mockMvc.perform(post("/wallets/5/debit").contentType(MediaType.APPLICATION_JSON)
				.content("{\"amount\":999999.00,\"currency\":\"USD\",\"idempotencyKey\":\"order-2\"}"))
				.andExpect(status().isPaymentRequired());
	}

	@Test
	void debit_withInvalidBody_returns400() throws Exception {
		mockMvc.perform(post("/wallets/5/debit").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void balance_withoutToken_returns200() throws Exception {
		when(walletService.getBalance(5L)).thenReturn(new BalanceResponse(new BigDecimal("50.00"), "USD"));

		mockMvc.perform(get("/wallets/5/balance")).andExpect(status().isOk());
	}

}
