package com.ejada.ecommerce.shop.client.dto;

import java.util.List;

public record InventoryReserveResponse(boolean reserved, List<UnavailableItem> shortfall) {
}
