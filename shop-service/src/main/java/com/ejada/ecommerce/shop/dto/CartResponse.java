package com.ejada.ecommerce.shop.dto;

import java.math.BigDecimal;
import java.util.List;

public record CartResponse(Long id, List<CartLineResponse> items, BigDecimal subtotal) {
}
