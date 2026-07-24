package com.ejada.ecommerce.shop.client.dto;

import java.math.BigDecimal;

public record BalanceResponse(BigDecimal balance, String currency) {
}
