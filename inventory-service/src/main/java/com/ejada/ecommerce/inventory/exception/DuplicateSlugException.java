package com.ejada.ecommerce.inventory.exception;

public class DuplicateSlugException extends RuntimeException {

	public DuplicateSlugException(String slug) {
		super("Category slug already exists: " + slug);
	}

}
