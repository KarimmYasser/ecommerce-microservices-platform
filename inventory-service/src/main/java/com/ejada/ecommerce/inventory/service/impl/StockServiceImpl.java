package com.ejada.ecommerce.inventory.service.impl;

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
import com.ejada.ecommerce.inventory.dto.UnavailableItem;
import com.ejada.ecommerce.inventory.exception.InvalidStockAdjustmentException;
import com.ejada.ecommerce.inventory.repository.InventoryReservationRepository;
import com.ejada.ecommerce.inventory.repository.StockItemRepository;
import com.ejada.ecommerce.inventory.service.StockService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class StockServiceImpl implements StockService {

	private final StockItemRepository stockItemRepository;
	private final InventoryReservationRepository reservationRepository;

	@Override
	@Transactional(readOnly = true)
	public InventoryCheckResponse check(InventoryCheckRequest request) {
		List<UnavailableItem> unavailable = new ArrayList<>();
		for (CheckItem item : request.items()) {
			StockItem stock = stockItemRepository.findByVariantId(item.variantId()).orElse(null);
			int available = stock != null ? stock.available() : 0;
			if (available < item.quantity()) {
				unavailable.add(new UnavailableItem(item.variantId(), item.quantity(), available));
			}
		}
		return new InventoryCheckResponse(unavailable.isEmpty(), unavailable);
	}

	@Override
	@Transactional
	public InventoryReserveResponse reserve(InventoryReserveRequest request) {
		Optional<InventoryReservation> existing = reservationRepository.findByOrderId(request.orderId());
		if (existing.isPresent()) {
			// Idempotent replay: this order already went through reserve (whatever the
			// current status), so re-running the same reservation is a no-op rather than
			// double-reserving stock or erroring on the retry.
			return InventoryReserveResponse.success();
		}

		// Lock in a stable order (ascending variantId) so two concurrent reserve
		// calls touching overlapping variants can never deadlock on each other.
		List<CheckItem> orderedItems = request.items().stream()
				.sorted(Comparator.comparing(CheckItem::variantId))
				.toList();

		List<StockItem> lockedStocks = new ArrayList<>();
		List<UnavailableItem> shortfall = new ArrayList<>();
		for (CheckItem item : orderedItems) {
			StockItem stock = stockItemRepository.findByVariantIdForUpdate(item.variantId()).orElse(null);
			int available = stock != null ? stock.available() : 0;
			if (available < item.quantity()) {
				shortfall.add(new UnavailableItem(item.variantId(), item.quantity(), available));
			} else {
				lockedStocks.add(stock);
			}
		}

		if (!shortfall.isEmpty()) {
			return InventoryReserveResponse.shortfall(shortfall);
		}

		InventoryReservation reservation = InventoryReservation.builder()
				.orderId(request.orderId())
				.status(ReservationStatus.RESERVED)
				.build();

		for (int i = 0; i < orderedItems.size(); i++) {
			CheckItem item = orderedItems.get(i);
			lockedStocks.get(i).reserve(item.quantity());
			reservation.addItem(InventoryReservationItem.builder()
					.variantId(item.variantId())
					.quantity(item.quantity())
					.build());
		}
		reservationRepository.save(reservation);

		return InventoryReserveResponse.success();
	}

	@Override
	@Transactional
	public InventoryReleaseResponse release(InventoryReleaseRequest request) {
		InventoryReservation reservation = reservationRepository.findByOrderId(request.orderId()).orElse(null);
		if (reservation == null || reservation.getStatus() == ReservationStatus.RELEASED) {
			// Nothing to do — safe no-op so a retried release can never fail or double-release.
			return new InventoryReleaseResponse(true);
		}

		for (InventoryReservationItem item : reservation.getItems()) {
			stockItemRepository.findByVariantIdForUpdate(item.getVariantId())
					.ifPresent(stock -> stock.release(item.getQuantity()));
		}
		reservation.setStatus(ReservationStatus.RELEASED);

		return new InventoryReleaseResponse(true);
	}

	@Override
	@Transactional
	public void adjustStock(Long variantId, int delta) {
		StockItem stock = stockItemRepository.findByVariantIdForUpdate(variantId)
				.orElseThrow(() -> new InvalidStockAdjustmentException("No stock record for variant: " + variantId));

		int newOnHand = stock.getQuantityOnHand() + delta;
		if (newOnHand < stock.getQuantityReserved()) {
			throw new InvalidStockAdjustmentException(
					"Adjustment would drop on-hand (%d) below reserved (%d) for variant %d"
							.formatted(newOnHand, stock.getQuantityReserved(), variantId));
		}
		stock.setQuantityOnHand(newOnHand);
	}

}
