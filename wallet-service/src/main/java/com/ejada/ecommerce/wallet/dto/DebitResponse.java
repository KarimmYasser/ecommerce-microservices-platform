package com.ejada.ecommerce.wallet.dto;

import java.math.BigDecimal;

public record DebitResponse(Long transactionId, BigDecimal balanceAfter) {
}
