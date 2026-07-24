package com.ejada.ecommerce.wallet.mapper;

import com.ejada.ecommerce.wallet.domain.WalletTransaction;
import com.ejada.ecommerce.wallet.dto.TransactionResponse;
import org.springframework.stereotype.Component;

@Component
public class WalletTransactionMapper {

	public TransactionResponse toResponse(WalletTransaction transaction) {
		return new TransactionResponse(
				transaction.getId(),
				transaction.getType(),
				transaction.getAmount(),
				transaction.getBalanceAfter(),
				transaction.getReferenceId(),
				transaction.getStatus(),
				transaction.getCreatedAt());
	}

}
