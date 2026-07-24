package com.ejada.ecommerce.wallet.dto;

import com.ejada.ecommerce.wallet.domain.TransactionStatus;
import com.ejada.ecommerce.wallet.domain.TransactionType;
import java.math.BigDecimal;
import java.time.Instant;

public record TransactionResponse(
		Long id,
		TransactionType type,
		BigDecimal amount,
		BigDecimal balanceAfter,
		String referenceId,
		TransactionStatus status,
		Instant createdAt) {
}
