package com.ejada.ecommerce.shop.client;

import com.ejada.ecommerce.shop.client.dto.BalanceResponse;
import com.ejada.ecommerce.shop.client.dto.CreditRequest;
import com.ejada.ecommerce.shop.client.dto.CreditResponse;
import com.ejada.ecommerce.shop.client.dto.DebitRequest;
import com.ejada.ecommerce.shop.client.dto.DebitResponse;
import com.ejada.ecommerce.shop.exception.PaymentFailedException;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Resilient wrapper for {@link WalletClient} that applies Resilience4j
 * circuit breaker and retry instances.
 */
@Component
@RequiredArgsConstructor
public class ResilientWalletClient {

	private final WalletClient walletClient;

	@CircuitBreaker(name = "walletDebit")
	public DebitResponse debit(Long userId, DebitRequest request) {
		try {
			return walletClient.debit(userId, request);
		} catch (FeignException ex) {
			if (ex.status() == 402) {
				throw new PaymentFailedException("Insufficient wallet funds for payment");
			}
			throw ex;
		}
	}

	@Retry(name = "walletCredit")
	public CreditResponse credit(Long userId, CreditRequest request) {
		return walletClient.credit(userId, request);
	}

	@CircuitBreaker(name = "walletDebit")
	public BalanceResponse getBalance(Long userId) {
		return walletClient.getBalance(userId);
	}

}
