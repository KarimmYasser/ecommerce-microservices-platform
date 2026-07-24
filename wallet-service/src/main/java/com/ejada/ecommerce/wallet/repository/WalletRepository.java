package com.ejada.ecommerce.wallet.repository;

import com.ejada.ecommerce.wallet.domain.Wallet;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface WalletRepository extends JpaRepository<Wallet, Long> {

	Optional<Wallet> findByUserId(Long userId);

	/**
	 * Pessimistic write lock so concurrent debit/credit calls for the same
	 * wallet serialize at the database — each waits for the lock and then
	 * mutates a genuinely fresh balance, rather than racing on an in-memory
	 * {@code @Version} check-then-write and needing a retry loop.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select w from Wallet w where w.user.id = :userId")
	Optional<Wallet> findByUserIdForUpdate(Long userId);

}
