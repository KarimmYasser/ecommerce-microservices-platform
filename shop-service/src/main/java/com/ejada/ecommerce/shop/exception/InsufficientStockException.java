package com.ejada.ecommerce.shop.exception;

import com.ejada.ecommerce.shop.client.dto.UnavailableItem;
import java.util.List;
import lombok.Getter;

@Getter
public class InsufficientStockException extends RuntimeException {

	private final List<UnavailableItem> unavailableItems;

	public InsufficientStockException(String message) {
		super(message);
		this.unavailableItems = List.of();
	}

	public InsufficientStockException(String message, List<UnavailableItem> unavailableItems) {
		super(message);
		this.unavailableItems = unavailableItems != null ? unavailableItems : List.of();
	}

}
