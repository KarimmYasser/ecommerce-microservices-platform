package com.ejada.ecommerce.inventory.exception;

import com.ejada.ecommerce.inventory.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler({ ProductNotFoundException.class, CategoryNotFoundException.class, VariantNotFoundException.class })
	public ResponseEntity<ErrorResponse> handleNotFound(RuntimeException ex, HttpServletRequest request) {
		return body(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage(), request);
	}

	@ExceptionHandler({ DuplicateSlugException.class, DuplicateSkuException.class })
	public ResponseEntity<ErrorResponse> handleConflict(RuntimeException ex, HttpServletRequest request) {
		return body(HttpStatus.CONFLICT, "CONFLICT", ex.getMessage(), request);
	}

	@ExceptionHandler(InvalidStockAdjustmentException.class)
	public ResponseEntity<ErrorResponse> handleInvalidStockAdjustment(InvalidStockAdjustmentException ex,
			HttpServletRequest request) {
		return body(HttpStatus.BAD_REQUEST, "INVALID_STOCK_ADJUSTMENT", ex.getMessage(), request);
	}

	@ExceptionHandler({ IllegalArgumentException.class, IllegalStateException.class })
	public ResponseEntity<ErrorResponse> handleBadRequest(RuntimeException ex, HttpServletRequest request) {
		return body(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.getMessage(), request);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex,
			HttpServletRequest request) {
		String message = ex.getBindingResult().getFieldErrors().stream()
				.map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
				.reduce((a, b) -> a + "; " + b)
				.orElse("Validation failed");
		return body(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", message, request);
	}

	private ResponseEntity<ErrorResponse> body(HttpStatus status, String error, String message,
			HttpServletRequest request) {
		return ResponseEntity.status(status)
				.body(ErrorResponse.of(status.value(), error, message, request.getRequestURI()));
	}

}
