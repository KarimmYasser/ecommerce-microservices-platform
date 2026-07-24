package com.ejada.ecommerce.inventory.service;

import com.ejada.ecommerce.inventory.dto.InventoryCheckRequest;
import com.ejada.ecommerce.inventory.dto.InventoryCheckResponse;
import com.ejada.ecommerce.inventory.dto.InventoryReleaseRequest;
import com.ejada.ecommerce.inventory.dto.InventoryReleaseResponse;
import com.ejada.ecommerce.inventory.dto.InventoryReserveRequest;
import com.ejada.ecommerce.inventory.dto.InventoryReserveResponse;

public interface StockService {

	InventoryCheckResponse check(InventoryCheckRequest request);

	/** Idempotent on {@code orderId} — a repeat call for an already-reserved order is a no-op. */
	InventoryReserveResponse reserve(InventoryReserveRequest request);

	/** Idempotent on {@code orderId} — releasing an unknown or already-released order is a no-op success. */
	InventoryReleaseResponse release(InventoryReleaseRequest request);

	void adjustStock(Long variantId, int delta);

}
