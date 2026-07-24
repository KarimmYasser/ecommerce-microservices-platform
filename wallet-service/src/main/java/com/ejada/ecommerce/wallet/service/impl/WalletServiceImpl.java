package com.ejada.ecommerce.wallet.service.impl;

import com.ejada.ecommerce.wallet.domain.TransactionStatus;
import com.ejada.ecommerce.wallet.domain.TransactionType;
import com.ejada.ecommerce.wallet.domain.Wallet;
import com.ejada.ecommerce.wallet.domain.WalletTransaction;
import com.ejada.ecommerce.wallet.dto.BalanceResponse;
import com.ejada.ecommerce.wallet.dto.CreditRequest;
import com.ejada.ecommerce.wallet.dto.CreditResponse;
import com.ejada.ecommerce.wallet.dto.DebitRequest;
import com.ejada.ecommerce.wallet.dto.DebitResponse;
import com.ejada.ecommerce.wallet.dto.PageResponse;
import com.ejada.ecommerce.wallet.dto.TransactionResponse;
import com.ejada.ecommerce.wallet.dto.WalletMutationResponse;
import com.ejada.ecommerce.wallet.dto.WalletResponse;
import com.ejada.ecommerce.wallet.exception.InsufficientFundsException;
import com.ejada.ecommerce.wallet.exception.UserNotFoundException;
import com.ejada.ecommerce.wallet.mapper.WalletTransactionMapper;
import com.ejada.ecommerce.wallet.repository.WalletRepository;
import com.ejada.ecommerce.wallet.repository.WalletTransactionRepository;
import com.ejada.ecommerce.wallet.service.WalletService;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class WalletServiceImpl implements WalletService {

	private final WalletRepository walletRepository;
	private final WalletTransactionRepository transactionRepository;
	private final WalletTransactionMapper transactionMapper;

	@Override
	@Transactional(readOnly = true)
	public WalletResponse getWallet(Long userId) {
		Wallet wallet = findWalletOrThrow(userId);
		return new WalletResponse(wallet.getBalance(), wallet.getCurrency());
	}

	@Override
	@Transactional
	public WalletMutationResponse deposit(Long userId, BigDecimal amount) {
		Wallet wallet = lockWalletOrThrow(userId);
		wallet.credit(amount);
		WalletTransaction transaction = recordTransaction(wallet, TransactionType.DEPOSIT, amount, null, null);
		return new WalletMutationResponse(wallet.getBalance(), transaction.getId());
	}

	@Override
	@Transactional
	public WalletMutationResponse withdraw(Long userId, BigDecimal amount) {
		Wallet wallet = lockWalletOrThrow(userId);
		if (!wallet.canDebit(amount)) {
			throw new InsufficientFundsException();
		}
		wallet.debit(amount);
		WalletTransaction transaction = recordTransaction(wallet, TransactionType.WITHDRAWAL, amount, null, null);
		return new WalletMutationResponse(wallet.getBalance(), transaction.getId());
	}

	@Override
	@Transactional(readOnly = true)
	public PageResponse<TransactionResponse> getTransactions(Long userId, Pageable pageable) {
		Wallet wallet = findWalletOrThrow(userId);
		var page = transactionRepository.findByWalletIdOrderByCreatedAtDesc(wallet.getId(), pageable)
				.map(transactionMapper::toResponse);
		return PageResponse.of(page);
	}

	@Override
	@Transactional
	public DebitResponse debit(Long userId, DebitRequest request) {
		var existing = transactionRepository.findByIdempotencyKey(request.idempotencyKey());
		if (existing.isPresent()) {
			WalletTransaction prior = existing.get();
			return new DebitResponse(prior.getId(), prior.getBalanceAfter());
		}

		Wallet wallet = lockWalletOrThrow(userId);
		if (!wallet.canDebit(request.amount())) {
			throw new InsufficientFundsException();
		}
		wallet.debit(request.amount());
		WalletTransaction transaction = recordTransaction(
				wallet, TransactionType.PAYMENT, request.amount(), request.idempotencyKey(), request.idempotencyKey());
		return new DebitResponse(transaction.getId(), wallet.getBalance());
	}

	@Override
	@Transactional
	public CreditResponse credit(Long userId, CreditRequest request) {
		var existing = transactionRepository.findByIdempotencyKey(request.idempotencyKey());
		if (existing.isPresent()) {
			WalletTransaction prior = existing.get();
			return new CreditResponse(prior.getId(), prior.getBalanceAfter());
		}

		Wallet wallet = lockWalletOrThrow(userId);
		wallet.credit(request.amount());
		WalletTransaction transaction = recordTransaction(
				wallet, TransactionType.REFUND, request.amount(), request.idempotencyKey(), request.idempotencyKey());
		return new CreditResponse(transaction.getId(), wallet.getBalance());
	}

	@Override
	@Transactional(readOnly = true)
	public BalanceResponse getBalance(Long userId) {
		Wallet wallet = findWalletOrThrow(userId);
		return new BalanceResponse(wallet.getBalance(), wallet.getCurrency());
	}

	private WalletTransaction recordTransaction(Wallet wallet, TransactionType type, BigDecimal amount,
			String referenceId, String idempotencyKey) {
		WalletTransaction transaction = WalletTransaction.builder()
				.wallet(wallet)
				.type(type)
				.amount(amount)
				.balanceAfter(wallet.getBalance())
				.referenceId(referenceId)
				.idempotencyKey(idempotencyKey)
				.status(TransactionStatus.SUCCESS)
				.build();
		return transactionRepository.save(transaction);
	}

	private Wallet findWalletOrThrow(Long userId) {
		return walletRepository.findByUserId(userId).orElseThrow(() -> new UserNotFoundException(userId));
	}

	private Wallet lockWalletOrThrow(Long userId) {
		return walletRepository.findByUserIdForUpdate(userId).orElseThrow(() -> new UserNotFoundException(userId));
	}

}
