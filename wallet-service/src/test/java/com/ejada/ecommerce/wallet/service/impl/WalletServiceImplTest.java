package com.ejada.ecommerce.wallet.service.impl;

import static com.ejada.ecommerce.wallet.support.EntityTestSupport.withId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ejada.ecommerce.wallet.domain.TransactionStatus;
import com.ejada.ecommerce.wallet.domain.Wallet;
import com.ejada.ecommerce.wallet.domain.WalletTransaction;
import com.ejada.ecommerce.wallet.dto.CreditRequest;
import com.ejada.ecommerce.wallet.dto.CreditResponse;
import com.ejada.ecommerce.wallet.dto.DebitRequest;
import com.ejada.ecommerce.wallet.dto.DebitResponse;
import com.ejada.ecommerce.wallet.dto.WalletMutationResponse;
import com.ejada.ecommerce.wallet.exception.InsufficientFundsException;
import com.ejada.ecommerce.wallet.exception.UserNotFoundException;
import com.ejada.ecommerce.wallet.mapper.WalletTransactionMapper;
import com.ejada.ecommerce.wallet.repository.WalletRepository;
import com.ejada.ecommerce.wallet.repository.WalletTransactionRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WalletServiceImplTest {

	@Mock
	private WalletRepository walletRepository;
	@Mock
	private WalletTransactionRepository transactionRepository;

	private WalletServiceImpl walletService;

	@BeforeEach
	void setUp() {
		walletService = new WalletServiceImpl(walletRepository, transactionRepository, new WalletTransactionMapper());
	}

	private Wallet wallet(long id, String balance) {
		return withId(Wallet.builder().balance(new BigDecimal(balance)).currency("USD").build(), id);
	}

	@Test
	void deposit_increasesBalanceAndRecordsTransaction() {
		Wallet wallet = wallet(1L, "100.00");
		when(walletRepository.findByUserIdForUpdate(5L)).thenReturn(Optional.of(wallet));
		when(transactionRepository.save(any(WalletTransaction.class)))
				.thenAnswer(inv -> withId(inv.getArgument(0), 200L));

		WalletMutationResponse response = walletService.deposit(5L, new BigDecimal("50.00"));

		assertThat(response.balance()).isEqualByComparingTo("150.00");
		assertThat(response.transactionId()).isEqualTo(200L);
	}

	@Test
	void withdraw_whenSufficientBalance_decreasesBalance() {
		Wallet wallet = wallet(1L, "100.00");
		when(walletRepository.findByUserIdForUpdate(5L)).thenReturn(Optional.of(wallet));
		when(transactionRepository.save(any(WalletTransaction.class)))
				.thenAnswer(inv -> withId(inv.getArgument(0), 201L));

		WalletMutationResponse response = walletService.withdraw(5L, new BigDecimal("40.00"));

		assertThat(response.balance()).isEqualByComparingTo("60.00");
	}

	@Test
	void withdraw_whenInsufficientBalance_throwsAndDoesNotMutate() {
		Wallet wallet = wallet(1L, "10.00");
		when(walletRepository.findByUserIdForUpdate(5L)).thenReturn(Optional.of(wallet));

		assertThatThrownBy(() -> walletService.withdraw(5L, new BigDecimal("40.00")))
				.isInstanceOf(InsufficientFundsException.class);
		assertThat(wallet.getBalance()).isEqualByComparingTo("10.00");
		verify(transactionRepository, never()).save(any());
	}

	@Test
	void debit_whenSufficientBalance_recordsPaymentAndReturnsNewBalance() {
		Wallet wallet = wallet(1L, "500.00");
		when(transactionRepository.findByIdempotencyKey("order-1")).thenReturn(Optional.empty());
		when(walletRepository.findByUserIdForUpdate(5L)).thenReturn(Optional.of(wallet));
		when(transactionRepository.save(any(WalletTransaction.class)))
				.thenAnswer(inv -> withId(inv.getArgument(0), 300L));

		DebitResponse response = walletService.debit(5L, new DebitRequest(new BigDecimal("200.00"), "USD", "order-1"));

		assertThat(response.balanceAfter()).isEqualByComparingTo("300.00");
		assertThat(response.transactionId()).isEqualTo(300L);
	}

	@Test
	void debit_whenInsufficientBalance_throwsInsufficientFundsAndRecordsNothing() {
		Wallet wallet = wallet(1L, "50.00");
		when(transactionRepository.findByIdempotencyKey("order-2")).thenReturn(Optional.empty());
		when(walletRepository.findByUserIdForUpdate(5L)).thenReturn(Optional.of(wallet));

		assertThatThrownBy(() -> walletService.debit(5L, new DebitRequest(new BigDecimal("200.00"), "USD", "order-2")))
				.isInstanceOf(InsufficientFundsException.class);
		verify(transactionRepository, never()).save(any());
	}

	@Test
	void debit_whenIdempotencyKeyAlreadyProcessed_returnsPriorResultWithoutMutating() {
		WalletTransaction prior = withId(WalletTransaction.builder()
				.type(com.ejada.ecommerce.wallet.domain.TransactionType.PAYMENT)
				.amount(new BigDecimal("200.00")).balanceAfter(new BigDecimal("300.00"))
				.idempotencyKey("order-1").status(TransactionStatus.SUCCESS).build(), 300L);
		when(transactionRepository.findByIdempotencyKey("order-1")).thenReturn(Optional.of(prior));

		DebitResponse response = walletService.debit(5L, new DebitRequest(new BigDecimal("200.00"), "USD", "order-1"));

		assertThat(response.transactionId()).isEqualTo(300L);
		assertThat(response.balanceAfter()).isEqualByComparingTo("300.00");
		verify(walletRepository, never()).findByUserIdForUpdate(org.mockito.ArgumentMatchers.anyLong());
	}

	@Test
	void credit_whenIdempotencyKeyAlreadyProcessed_returnsPriorResultWithoutMutating() {
		WalletTransaction prior = withId(WalletTransaction.builder()
				.type(com.ejada.ecommerce.wallet.domain.TransactionType.REFUND)
				.amount(new BigDecimal("200.00")).balanceAfter(new BigDecimal("300.00"))
				.idempotencyKey("refund-1").status(TransactionStatus.SUCCESS).build(), 301L);
		when(transactionRepository.findByIdempotencyKey("refund-1")).thenReturn(Optional.of(prior));

		CreditResponse response = walletService.credit(5L, new CreditRequest(new BigDecimal("200.00"), "USD", "refund-1"));

		assertThat(response.transactionId()).isEqualTo(301L);
		verify(walletRepository, never()).findByUserIdForUpdate(org.mockito.ArgumentMatchers.anyLong());
	}

	@Test
	void credit_whenNewIdempotencyKey_increasesBalance() {
		Wallet wallet = wallet(1L, "100.00");
		when(transactionRepository.findByIdempotencyKey("refund-2")).thenReturn(Optional.empty());
		when(walletRepository.findByUserIdForUpdate(5L)).thenReturn(Optional.of(wallet));
		when(transactionRepository.save(any(WalletTransaction.class)))
				.thenAnswer(inv -> withId(inv.getArgument(0), 302L));

		CreditResponse response = walletService.credit(5L, new CreditRequest(new BigDecimal("50.00"), "USD", "refund-2"));

		assertThat(response.balanceAfter()).isEqualByComparingTo("150.00");
	}

	@Test
	void getWallet_whenMissing_throwsUserNotFoundException() {
		when(walletRepository.findByUserId(404L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> walletService.getWallet(404L)).isInstanceOf(UserNotFoundException.class);
	}

}
