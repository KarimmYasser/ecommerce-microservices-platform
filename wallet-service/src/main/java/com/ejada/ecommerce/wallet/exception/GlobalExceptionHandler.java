package com.ejada.ecommerce.wallet.exception;

import com.ejada.ecommerce.wallet.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(UserNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleNotFound(UserNotFoundException ex, HttpServletRequest request) {
		return body(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage(), request);
	}

	@ExceptionHandler(DuplicateEmailException.class)
	public ResponseEntity<ErrorResponse> handleDuplicateEmail(DuplicateEmailException ex, HttpServletRequest request) {
		return body(HttpStatus.CONFLICT, "CONFLICT", ex.getMessage(), request);
	}

	@ExceptionHandler(InvalidCredentialsException.class)
	public ResponseEntity<ErrorResponse> handleInvalidCredentials(InvalidCredentialsException ex, HttpServletRequest request) {
		return body(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", ex.getMessage(), request);
	}

	@ExceptionHandler(InsufficientFundsException.class)
	public ResponseEntity<ErrorResponse> handleInsufficientFunds(InsufficientFundsException ex, HttpServletRequest request) {
		return body(HttpStatus.PAYMENT_REQUIRED, "INSUFFICIENT_FUNDS", ex.getMessage(), request);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
		String message = ex.getBindingResult().getFieldErrors().stream()
				.map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
				.reduce((a, b) -> a + "; " + b)
				.orElse("Validation failed");
		return body(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", message, request);
	}

	private ResponseEntity<ErrorResponse> body(HttpStatus status, String error, String message, HttpServletRequest request) {
		return ResponseEntity.status(status).body(ErrorResponse.of(status.value(), error, message, request.getRequestURI()));
	}

}
