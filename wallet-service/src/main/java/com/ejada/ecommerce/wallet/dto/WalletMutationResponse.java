package com.ejada.ecommerce.wallet.dto;

import java.math.BigDecimal;

public record WalletMutationResponse(BigDecimal balance, Long transactionId) {
}
