package com.ejada.ecommerce.inventory.controller;

import com.ejada.ecommerce.inventory.dto.InventoryCheckRequest;
import com.ejada.ecommerce.inventory.dto.InventoryCheckResponse;
import com.ejada.ecommerce.inventory.dto.InventoryReleaseRequest;
import com.ejada.ecommerce.inventory.dto.InventoryReleaseResponse;
import com.ejada.ecommerce.inventory.dto.InventoryReserveRequest;
import com.ejada.ecommerce.inventory.dto.InventoryReserveResponse;
import com.ejada.ecommerce.inventory.service.StockService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Service-to-service only — called by shop-service's Feign clients during the
 * checkout saga. The gateway does not route {@code /inventory/**} publicly;
 * see docs/infrastructure/api-gateway.md.
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/inventory")
public class InventoryInternalController {

	private final StockService stockService;

	@PostMapping("/check")
	public InventoryCheckResponse check(@Valid @RequestBody InventoryCheckRequest request) {
		return stockService.check(request);
	}

	@PostMapping("/reserve")
	public ResponseEntity<InventoryReserveResponse> reserve(@Valid @RequestBody InventoryReserveRequest request) {
		InventoryReserveResponse response = stockService.reserve(request);
		HttpStatus status = response.reserved() ? HttpStatus.OK : HttpStatus.CONFLICT;
		return ResponseEntity.status(status).body(response);
	}

	@PostMapping("/release")
	public InventoryReleaseResponse release(@Valid @RequestBody InventoryReleaseRequest request) {
		return stockService.release(request);
	}

}
