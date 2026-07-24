package com.ejada.ecommerce.inventory.exception;

public class VariantNotFoundException extends RuntimeException {

	public VariantNotFoundException(Long id) {
		super("Product variant not found: " + id);
	}

}
