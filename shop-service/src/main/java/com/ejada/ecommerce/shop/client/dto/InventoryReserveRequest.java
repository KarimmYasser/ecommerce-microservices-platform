package com.ejada.ecommerce.shop.client.dto;

import java.util.List;

public record InventoryReserveRequest(Long orderId, List<CheckItem> items) {
}
