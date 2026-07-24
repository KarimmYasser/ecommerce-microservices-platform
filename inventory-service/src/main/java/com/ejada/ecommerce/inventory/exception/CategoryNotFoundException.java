package com.ejada.ecommerce.inventory.exception;

public class CategoryNotFoundException extends RuntimeException {

	public CategoryNotFoundException(Long id) {
		super("Category not found: " + id);
	}

}
