package com.ejada.ecommerce.wallet.controller;

import com.ejada.ecommerce.wallet.dto.AmountRequest;
import com.ejada.ecommerce.wallet.dto.PageResponse;
import com.ejada.ecommerce.wallet.dto.TransactionResponse;
import com.ejada.ecommerce.wallet.dto.WalletMutationResponse;
import com.ejada.ecommerce.wallet.dto.WalletResponse;
import com.ejada.ecommerce.wallet.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/wallets")
public class WalletController {

	private final WalletService walletService;

	@GetMapping("/me")
	public WalletResponse getMyWallet(Authentication authentication) {
		return walletService.getWallet(currentUserId(authentication));
	}

	@PostMapping("/me/deposit")
	public WalletMutationResponse deposit(Authentication authentication, @Valid @RequestBody AmountRequest request) {
		return walletService.deposit(currentUserId(authentication), request.amount());
	}

	@PostMapping("/me/withdraw")
	public WalletMutationResponse withdraw(Authentication authentication, @Valid @RequestBody AmountRequest request) {
		return walletService.withdraw(currentUserId(authentication), request.amount());
	}

	@GetMapping("/me/transactions")
	public PageResponse<TransactionResponse> getTransactions(Authentication authentication, @org.springdoc.core.annotations.ParameterObject Pageable pageable) {
		return walletService.getTransactions(currentUserId(authentication), pageable);
	}

	private Long currentUserId(Authentication authentication) {
		return Long.valueOf(authentication.getName());
	}

}
