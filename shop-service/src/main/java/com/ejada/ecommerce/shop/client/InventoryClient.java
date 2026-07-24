package com.ejada.ecommerce.shop.client;

import com.ejada.ecommerce.shop.client.dto.InventoryReleaseRequest;
import com.ejada.ecommerce.shop.client.dto.InventoryReleaseResponse;
import com.ejada.ecommerce.shop.client.dto.InventoryReserveRequest;
import com.ejada.ecommerce.shop.client.dto.InventoryReserveResponse;
import com.ejada.ecommerce.shop.client.dto.ProductBatchItem;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * See docs/api/inter-service-feign.md. {@code reserve} throws
 * {@code FeignException} with status 409 on a shortfall (Spring Cloud
 * OpenFeign throws for any non-2xx regardless of declared return type) — the
 * caller distinguishes "not enough stock" from "inventory unavailable" by
 * catching and inspecting the exception, not by return value.
 */
@FeignClient(name = "inventory-service")
public interface InventoryClient {

	@GetMapping("/api/v1/products/batch")
	List<ProductBatchItem> getProductsBatch(@RequestParam("ids") List<Long> ids);

	@PostMapping("/inventory/reserve")
	InventoryReserveResponse reserve(@RequestBody InventoryReserveRequest request);

	@PostMapping("/inventory/release")
	InventoryReleaseResponse release(@RequestBody InventoryReleaseRequest request);

}
