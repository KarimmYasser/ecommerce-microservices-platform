package com.ejada.ecommerce.shop.client;

import com.ejada.ecommerce.shop.client.dto.InventoryReleaseRequest;
import com.ejada.ecommerce.shop.client.dto.InventoryReleaseResponse;
import com.ejada.ecommerce.shop.client.dto.InventoryReserveRequest;
import com.ejada.ecommerce.shop.client.dto.InventoryReserveResponse;
import com.ejada.ecommerce.shop.client.dto.ProductBatchItem;
import com.ejada.ecommerce.shop.exception.InsufficientStockException;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Resilient wrapper for {@link InventoryClient} that applies Resilience4j
 * circuit breaker and retry instances.
 */
@Component
@RequiredArgsConstructor
public class ResilientInventoryClient {

	private final InventoryClient inventoryClient;

	@CircuitBreaker(name = "inventoryCheck")
	public List<ProductBatchItem> getProductsBatch(List<Long> ids) {
		return inventoryClient.getProductsBatch(ids);
	}

	@CircuitBreaker(name = "inventoryReserve")
	public InventoryReserveResponse reserve(InventoryReserveRequest request) {
		try {
			return inventoryClient.reserve(request);
		} catch (FeignException ex) {
			if (ex.status() == 409) {
				throw new InsufficientStockException("Insufficient stock for checkout items");
			}
			throw ex;
		}
	}

	@Retry(name = "inventoryRelease")
	public InventoryReleaseResponse release(InventoryReleaseRequest request) {
		return inventoryClient.release(request);
	}

}
