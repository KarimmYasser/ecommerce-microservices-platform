package com.ejada.ecommerce.shop.client.dto;

import java.math.BigDecimal;

public record CreditRequest(BigDecimal amount, String currency, String idempotencyKey) {
}
