package com.ejada.ecommerce.wallet.dto;

import java.math.BigDecimal;

public record CreditResponse(Long transactionId, BigDecimal balanceAfter) {
}
