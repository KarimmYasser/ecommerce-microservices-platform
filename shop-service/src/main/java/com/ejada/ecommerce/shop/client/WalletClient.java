package com.ejada.ecommerce.shop.client;

import com.ejada.ecommerce.shop.client.dto.BalanceResponse;
import com.ejada.ecommerce.shop.client.dto.CreditRequest;
import com.ejada.ecommerce.shop.client.dto.CreditResponse;
import com.ejada.ecommerce.shop.client.dto.DebitRequest;
import com.ejada.ecommerce.shop.client.dto.DebitResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * See docs/api/inter-service-feign.md. {@code debit} throws
 * {@code FeignException} with status 402 on insufficient funds.
 */
@FeignClient(name = "wallet-service")
public interface WalletClient {

	@PostMapping("/wallets/{userId}/debit")
	DebitResponse debit(@PathVariable("userId") Long userId, @RequestBody DebitRequest request);

	@PostMapping("/wallets/{userId}/credit")
	CreditResponse credit(@PathVariable("userId") Long userId, @RequestBody CreditRequest request);

	@GetMapping("/wallets/{userId}/balance")
	BalanceResponse getBalance(@PathVariable("userId") Long userId);

}
