package com.ejada.ecommerce.wallet.exception;

public class InsufficientFundsException extends RuntimeException {

	public InsufficientFundsException() {
		super("Insufficient funds");
	}

}
