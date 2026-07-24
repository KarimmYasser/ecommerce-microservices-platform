package com.ejada.ecommerce.wallet.controller;

import com.ejada.ecommerce.wallet.dto.BalanceResponse;
import com.ejada.ecommerce.wallet.dto.CreditRequest;
import com.ejada.ecommerce.wallet.dto.CreditResponse;
import com.ejada.ecommerce.wallet.dto.DebitRequest;
import com.ejada.ecommerce.wallet.dto.DebitResponse;
import com.ejada.ecommerce.wallet.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Service-to-service only — called by shop-service's Feign clients during the
 * checkout saga. The gateway does not route these publicly; see
 * docs/infrastructure/api-gateway.md.
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/wallets")
public class WalletInternalController {

	private final WalletService walletService;

	@PostMapping("/{userId}/debit")
	public DebitResponse debit(@PathVariable Long userId, @Valid @RequestBody DebitRequest request) {
		return walletService.debit(userId, request);
	}

	@PostMapping("/{userId}/credit")
	public CreditResponse credit(@PathVariable Long userId, @Valid @RequestBody CreditRequest request) {
		return walletService.credit(userId, request);
	}

	@GetMapping("/{userId}/balance")
	public BalanceResponse balance(@PathVariable Long userId) {
		return walletService.getBalance(userId);
	}

}
