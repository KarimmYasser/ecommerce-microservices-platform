package com.ejada.ecommerce.shop.client.dto;

import java.math.BigDecimal;

public record DebitResponse(Long transactionId, BigDecimal balanceAfter) {
}
