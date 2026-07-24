package com.ejada.ecommerce.inventory.repository;

import com.ejada.ecommerce.inventory.domain.InventoryReservation;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, Long> {

	Optional<InventoryReservation> findByOrderId(Long orderId);

}
