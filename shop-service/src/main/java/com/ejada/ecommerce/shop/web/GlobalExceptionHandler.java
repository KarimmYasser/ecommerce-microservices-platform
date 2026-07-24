package com.ejada.ecommerce.shop.web;

import com.ejada.ecommerce.shop.dto.ErrorResponse;
import com.ejada.ecommerce.shop.exception.DownstreamServiceException;
import com.ejada.ecommerce.shop.exception.DuplicateResourceException;
import com.ejada.ecommerce.shop.exception.InsufficientStockException;
import com.ejada.ecommerce.shop.exception.PaymentFailedException;
import com.ejada.ecommerce.shop.exception.ResourceNotFoundException;
import feign.FeignException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(ErrorResponse.of(HttpStatus.NOT_FOUND.value(), "NOT_FOUND", ex.getMessage(), request.getRequestURI()));
	}

	@ExceptionHandler(InsufficientStockException.class)
	public ResponseEntity<ErrorResponse> handleStockShortfall(InsufficientStockException ex, HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(ErrorResponse.of(HttpStatus.CONFLICT.value(), "OUT_OF_STOCK", ex.getMessage(), request.getRequestURI()));
	}

	@ExceptionHandler(PaymentFailedException.class)
	public ResponseEntity<ErrorResponse> handlePaymentFailed(PaymentFailedException ex, HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
				.body(ErrorResponse.of(HttpStatus.PAYMENT_REQUIRED.value(), "PAYMENT_FAILED", ex.getMessage(), request.getRequestURI()));
	}

	@ExceptionHandler(DuplicateResourceException.class)
	public ResponseEntity<ErrorResponse> handleDuplicate(DuplicateResourceException ex, HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(ErrorResponse.of(HttpStatus.CONFLICT.value(), "DUPLICATE_RESOURCE", ex.getMessage(), request.getRequestURI()));
	}

	@ExceptionHandler(DownstreamServiceException.class)
	public ResponseEntity<ErrorResponse> handleDownstream(DownstreamServiceException ex, HttpServletRequest request) {
		log.warn("Downstream service failure: {}", ex.getMessage());
		return ResponseEntity.status(HttpStatus.FAILED_DEPENDENCY)
				.body(ErrorResponse.of(HttpStatus.FAILED_DEPENDENCY.value(), "DOWNSTREAM_UNAVAILABLE", ex.getMessage(), request.getRequestURI()));
	}

	@ExceptionHandler(FeignException.class)
	public ResponseEntity<ErrorResponse> handleFeign(FeignException ex, HttpServletRequest request) {
		int status = ex.status() > 0 ? ex.status() : HttpStatus.FAILED_DEPENDENCY.value();
		String error = status == 409 ? "OUT_OF_STOCK" : status == 402 ? "PAYMENT_FAILED" : "DOWNSTREAM_UNAVAILABLE";
		return ResponseEntity.status(status)
				.body(ErrorResponse.of(status, error, ex.getMessage(), request.getRequestURI()));
	}

	@ExceptionHandler({ IllegalArgumentException.class, IllegalStateException.class })
	public ResponseEntity<ErrorResponse> handleBadRequest(RuntimeException ex, HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(ErrorResponse.of(HttpStatus.BAD_REQUEST.value(), "BAD_REQUEST", ex.getMessage(), request.getRequestURI()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
		String details = ex.getBindingResult().getFieldErrors().stream()
				.map(error -> error.getField() + ": " + error.getDefaultMessage())
				.collect(Collectors.joining(", "));
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(ErrorResponse.of(HttpStatus.BAD_REQUEST.value(), "VALIDATION_FAILED", details, request.getRequestURI()));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
		log.error("Unhandled exception processing request {}", request.getRequestURI(), ex);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(ErrorResponse.of(HttpStatus.INTERNAL_SERVER_ERROR.value(), "INTERNAL_SERVER_ERROR", "An unexpected error occurred", request.getRequestURI()));
	}

}
