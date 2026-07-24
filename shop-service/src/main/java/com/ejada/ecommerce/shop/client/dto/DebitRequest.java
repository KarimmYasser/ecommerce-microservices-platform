package com.ejada.ecommerce.shop.client.dto;

import java.math.BigDecimal;

public record DebitRequest(BigDecimal amount, String currency, String idempotencyKey) {
}
