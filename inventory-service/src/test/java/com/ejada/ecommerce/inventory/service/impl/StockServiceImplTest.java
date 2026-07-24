package com.ejada.ecommerce.inventory.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ejada.ecommerce.inventory.domain.InventoryReservation;
import com.ejada.ecommerce.inventory.domain.InventoryReservationItem;
import com.ejada.ecommerce.inventory.domain.ReservationStatus;
import com.ejada.ecommerce.inventory.domain.StockItem;
import com.ejada.ecommerce.inventory.dto.CheckItem;
import com.ejada.ecommerce.inventory.dto.InventoryCheckRequest;
import com.ejada.ecommerce.inventory.dto.InventoryCheckResponse;
import com.ejada.ecommerce.inventory.dto.InventoryReleaseRequest;
import com.ejada.ecommerce.inventory.dto.InventoryReleaseResponse;
import com.ejada.ecommerce.inventory.dto.InventoryReserveRequest;
import com.ejada.ecommerce.inventory.dto.InventoryReserveResponse;
import com.ejada.ecommerce.inventory.exception.InvalidStockAdjustmentException;
import com.ejada.ecommerce.inventory.repository.InventoryReservationRepository;
import com.ejada.ecommerce.inventory.repository.StockItemRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StockServiceImplTest {

	@Mock
	private StockItemRepository stockItemRepository;

	@Mock
	private InventoryReservationRepository reservationRepository;

	private StockServiceImpl stockService;

	@BeforeEach
	void setUp() {
		stockService = new StockServiceImpl(stockItemRepository, reservationRepository);
	}

	private StockItem stockItem(long onHand, long reserved) {
		return StockItem.builder().quantityOnHand((int) onHand).quantityReserved((int) reserved).build();
	}

	// --- check() ---

	@Test
	void check_whenAllAvailable_returnsAllAvailableTrue() {
		when(stockItemRepository.findByVariantId(1L)).thenReturn(Optional.of(stockItem(10, 2)));

		InventoryCheckResponse response = stockService.check(new InventoryCheckRequest(List.of(new CheckItem(1L, 5))));

		assertThat(response.allAvailable()).isTrue();
		assertThat(response.unavailable()).isEmpty();
	}

	@Test
	void check_whenInsufficientStock_returnsUnavailableItem() {
		when(stockItemRepository.findByVariantId(1L)).thenReturn(Optional.of(stockItem(10, 8)));

		InventoryCheckResponse response = stockService.check(new InventoryCheckRequest(List.of(new CheckItem(1L, 5))));

		assertThat(response.allAvailable()).isFalse();
		assertThat(response.unavailable()).hasSize(1);
		assertThat(response.unavailable().get(0).variantId()).isEqualTo(1L);
		assertThat(response.unavailable().get(0).available()).isEqualTo(2);
	}

	@Test
	void check_whenVariantUnknown_treatsAsUnavailable() {
		when(stockItemRepository.findByVariantId(99L)).thenReturn(Optional.empty());

		InventoryCheckResponse response = stockService.check(new InventoryCheckRequest(List.of(new CheckItem(99L, 1))));

		assertThat(response.allAvailable()).isFalse();
		assertThat(response.unavailable().get(0).available()).isZero();
	}

	// --- reserve() ---

	@Test
	void reserve_whenSufficientStock_reservesAndReturnsSuccess() {
		when(reservationRepository.findByOrderId(100L)).thenReturn(Optional.empty());
		StockItem stock = stockItem(10, 0);
		when(stockItemRepository.findByVariantIdForUpdate(1L)).thenReturn(Optional.of(stock));

		InventoryReserveResponse response = stockService.reserve(
				new InventoryReserveRequest(100L, List.of(new CheckItem(1L, 4))));

		assertThat(response.reserved()).isTrue();
		assertThat(stock.getQuantityReserved()).isEqualTo(4);
		verify(reservationRepository).save(any(InventoryReservation.class));
	}

	@Test
	void reserve_whenInsufficientStock_returnsShortfallWithoutMutating() {
		when(reservationRepository.findByOrderId(100L)).thenReturn(Optional.empty());
		StockItem stock = stockItem(3, 0);
		when(stockItemRepository.findByVariantIdForUpdate(1L)).thenReturn(Optional.of(stock));

		InventoryReserveResponse response = stockService.reserve(
				new InventoryReserveRequest(100L, List.of(new CheckItem(1L, 5))));

		assertThat(response.reserved()).isFalse();
		assertThat(response.shortfall()).hasSize(1);
		assertThat(stock.getQuantityReserved()).isZero();
		verify(reservationRepository, never()).save(any());
	}

	@Test
	void reserve_whenOneOfSeveralItemsIsShort_noItemIsReserved() {
		when(reservationRepository.findByOrderId(100L)).thenReturn(Optional.empty());
		StockItem plentiful = stockItem(10, 0);
		StockItem scarce = stockItem(1, 0);
		when(stockItemRepository.findByVariantIdForUpdate(1L)).thenReturn(Optional.of(plentiful));
		when(stockItemRepository.findByVariantIdForUpdate(2L)).thenReturn(Optional.of(scarce));

		InventoryReserveResponse response = stockService.reserve(new InventoryReserveRequest(
				100L, List.of(new CheckItem(1L, 5), new CheckItem(2L, 5))));

		assertThat(response.reserved()).isFalse();
		assertThat(plentiful.getQuantityReserved())
				.as("the plentiful item must not be reserved when a sibling item in the same order is short")
				.isZero();
		assertThat(scarce.getQuantityReserved()).isZero();
	}

	@Test
	void reserve_whenOrderIdAlreadyReserved_isIdempotentNoOp() {
		InventoryReservation existing = InventoryReservation.builder()
				.orderId(100L).status(ReservationStatus.RESERVED).build();
		when(reservationRepository.findByOrderId(100L)).thenReturn(Optional.of(existing));

		InventoryReserveResponse response = stockService.reserve(
				new InventoryReserveRequest(100L, List.of(new CheckItem(1L, 4))));

		assertThat(response.reserved()).isTrue();
		verify(stockItemRepository, never()).findByVariantIdForUpdate(anyLong());
		verify(reservationRepository, never()).save(any());
	}

	// --- release() ---

	@Test
	void release_whenReservationExists_releasesStockAndMarksReleased() {
		StockItem stock = stockItem(10, 4);
		InventoryReservation reservation = InventoryReservation.builder()
				.orderId(100L).status(ReservationStatus.RESERVED).build();
		reservation.addItem(InventoryReservationItem.builder().variantId(1L).quantity(4).build());
		when(reservationRepository.findByOrderId(100L)).thenReturn(Optional.of(reservation));
		when(stockItemRepository.findByVariantIdForUpdate(1L)).thenReturn(Optional.of(stock));

		InventoryReleaseResponse response = stockService.release(new InventoryReleaseRequest(100L));

		assertThat(response.released()).isTrue();
		assertThat(stock.getQuantityReserved()).isZero();
		assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.RELEASED);
	}

	@Test
	void release_whenReservationNotFound_isIdempotentNoOp() {
		when(reservationRepository.findByOrderId(100L)).thenReturn(Optional.empty());

		InventoryReleaseResponse response = stockService.release(new InventoryReleaseRequest(100L));

		assertThat(response.released()).isTrue();
		verify(stockItemRepository, never()).findByVariantIdForUpdate(anyLong());
	}

	@Test
	void release_whenAlreadyReleased_isIdempotentNoOp() {
		InventoryReservation reservation = InventoryReservation.builder()
				.orderId(100L).status(ReservationStatus.RELEASED).build();
		when(reservationRepository.findByOrderId(100L)).thenReturn(Optional.of(reservation));

		InventoryReleaseResponse response = stockService.release(new InventoryReleaseRequest(100L));

		assertThat(response.released()).isTrue();
		verify(stockItemRepository, never()).findByVariantIdForUpdate(anyLong());
	}

	// --- adjustStock() ---

	@Test
	void adjustStock_whenPositiveDelta_increasesOnHand() {
		StockItem stock = stockItem(10, 2);
		when(stockItemRepository.findByVariantIdForUpdate(1L)).thenReturn(Optional.of(stock));

		stockService.adjustStock(1L, 5);

		assertThat(stock.getQuantityOnHand()).isEqualTo(15);
	}

	@Test
	void adjustStock_whenNegativeDeltaDropsBelowReserved_throws() {
		StockItem stock = stockItem(10, 8);
		when(stockItemRepository.findByVariantIdForUpdate(1L)).thenReturn(Optional.of(stock));

		assertThatThrownBy(() -> stockService.adjustStock(1L, -5))
				.isInstanceOf(InvalidStockAdjustmentException.class);
		assertThat(stock.getQuantityOnHand()).as("must not mutate on rejection").isEqualTo(10);
	}

	@Test
	void adjustStock_whenVariantUnknown_throws() {
		when(stockItemRepository.findByVariantIdForUpdate(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> stockService.adjustStock(99L, 1))
				.isInstanceOf(InvalidStockAdjustmentException.class);
	}

}
