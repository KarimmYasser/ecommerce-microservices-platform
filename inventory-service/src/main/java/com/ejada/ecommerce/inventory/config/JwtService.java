package com.ejada.ecommerce.inventory.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Validates JWTs issued by wallet-service. inventory-service never signs a
 * token itself — see docs/security/authentication-authorization.md.
 */
@Component
public class JwtService {

	@Value("${jwt.secret}")
	private String secret;

	private SecretKey key;

	@PostConstruct
	void init() {
		this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
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
