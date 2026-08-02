package com.ejada.ecommerce.wallet.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * The identity provider: this is the only service that SIGNS tokens. Every
 * other service only validates them with the same shared secret. See
 * docs/security/authentication-authorization.md.
 */
@Component
public class JwtService {

	@Value("${jwt.secret}")
	private String secret;

	@Value("${jwt.expiration-ms}")
	private long expirationMs;

	private SecretKey key;

	@PostConstruct
	void init() {
		this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
	}

	public String issue(Long userId, String role) {
		Instant now = Instant.now();
		return Jwts.builder()
				.id(UUID.randomUUID().toString())
				.subject(String.valueOf(userId))
				.claim("roles", List.of(role))
				.issuedAt(Date.from(now))
				.expiration(Date.from(now.plusMillis(expirationMs)))
				.signWith(key)
				.compact();
	}

	public long expirationSeconds() {
		return expirationMs / 1000;
	}

	/** Empty if the token is missing, malformed, expired, or has a bad signature. */
	public Optional<AuthenticatedUser> validate(String token) {
		try {
			Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
			List<?> rawRoles = claims.get("roles", List.class);
			List<String> roles = rawRoles == null ? List.of() : rawRoles.stream().map(Object::toString).toList();
			return Optional.of(new AuthenticatedUser(claims.getSubject(), roles));
		} catch (JwtException | IllegalArgumentException ex) {
			return Optional.empty();
		}
	}

	public record AuthenticatedUser(String userId, List<String> roles) {
	}

}
