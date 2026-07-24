package com.ejada.ecommerce.shop.client.dto;

import java.math.BigDecimal;

public record CreditResponse(Long transactionId, BigDecimal balanceAfter) {
}
