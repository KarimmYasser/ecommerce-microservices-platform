package com.ejada.ecommerce.inventory.controller;

import com.ejada.ecommerce.inventory.dto.StockAdjustRequest;
import com.ejada.ecommerce.inventory.service.StockService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/variants")
public class VariantController {

	private final StockService stockService;

	@PostMapping("/{variantId}/stock/adjust")
	public ResponseEntity<Void> adjustStock(@PathVariable Long variantId, @Valid @RequestBody StockAdjustRequest request) {
		stockService.adjustStock(variantId, request.delta());
		return ResponseEntity.noContent().build();
	}

}
