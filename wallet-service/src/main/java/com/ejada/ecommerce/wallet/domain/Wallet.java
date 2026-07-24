package com.ejada.ecommerce.wallet.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "wallets")
public class Wallet extends BaseEntity {

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false, unique = true)
	private User user;

	@Column(name = "balance", nullable = false, precision = 12, scale = 2)
	@Builder.Default
	private BigDecimal balance = BigDecimal.ZERO;

	@Column(name = "currency", nullable = false, length = 3)
	private String currency;

	/** Guards concurrent debit/credit against lost updates; see WalletServiceImpl's retry loop. */
	@Version
	@Column(name = "version", nullable = false)
	@Builder.Default
	private long version = 0L;

	public boolean canDebit(BigDecimal amount) {
		return balance.compareTo(amount) >= 0;
	}

	public void debit(BigDecimal amount) {
		if (!canDebit(amount)) {
			throw new IllegalStateException("Cannot debit %s; balance is %s".formatted(amount, balance));
		}
		balance = balance.subtract(amount);
	}

	public void credit(BigDecimal amount) {
		balance = balance.add(amount);
	}

}
