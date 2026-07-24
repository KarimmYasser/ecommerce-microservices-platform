package com.ejada.ecommerce.inventory.exception;

public class DuplicateSkuException extends RuntimeException {

	public DuplicateSkuException(String sku) {
		super("Variant SKU already exists: " + sku);
	}

}
