package com.ejada.ecommerce.wallet.service;

import com.ejada.ecommerce.wallet.dto.BalanceResponse;
import com.ejada.ecommerce.wallet.dto.CreditRequest;
import com.ejada.ecommerce.wallet.dto.CreditResponse;
import com.ejada.ecommerce.wallet.dto.DebitRequest;
import com.ejada.ecommerce.wallet.dto.DebitResponse;
import com.ejada.ecommerce.wallet.dto.PageResponse;
import com.ejada.ecommerce.wallet.dto.TransactionResponse;
import com.ejada.ecommerce.wallet.dto.WalletMutationResponse;
import com.ejada.ecommerce.wallet.dto.WalletResponse;
import java.math.BigDecimal;
import org.springframework.data.domain.Pageable;

public interface WalletService {

	WalletResponse getWallet(Long userId);

	WalletMutationResponse deposit(Long userId, BigDecimal amount);

	WalletMutationResponse withdraw(Long userId, BigDecimal amount);

	PageResponse<TransactionResponse> getTransactions(Long userId, Pageable pageable);

	/** Idempotent on {@code request.idempotencyKey()} — a retried debit never double-charges. */
	DebitResponse debit(Long userId, DebitRequest request);

	/** Idempotent on {@code request.idempotencyKey()}. */
	CreditResponse credit(Long userId, CreditRequest request);

	BalanceResponse getBalance(Long userId);

}
