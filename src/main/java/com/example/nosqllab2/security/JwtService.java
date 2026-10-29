package com.example.nosqllab2.security;

import com.example.nosqllab2.entity.Role;
import com.example.nosqllab2.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;

@Component
public class JwtService {

    private static final String ROLES_CLAIM = "roles";
    private static final String USER_ID_CLAIM = "id";

    private final SecretKey signingKey;
    private final Duration ttl;

    public JwtService(@Value("${app.jwt.secret}") String secret, @Value("${app.jwt.ttl}") Duration ttl) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.ttl = ttl;
    }

    public String generateToken(User user) {
        Instant now = Instant.now();

        List<String> roles = user.getRoles().stream()
                .map(Role::getValue)
                .toList();

        return Jwts.builder()
                .subject(user.getUsername())
                .claim(USER_ID_CLAIM, user.getId())
                .claim(ROLES_CLAIM, roles)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(signingKey)
                .compact();
    }

    @SuppressWarnings("unchecked")
    public AuthenticatedUser parseToken(String token) throws JwtException {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        List<String> roles = claims.get(ROLES_CLAIM, List.class);

        return new AuthenticatedUser(
                claims.get(USER_ID_CLAIM, Number.class).longValue(),
                claims.getSubject(),
                roles == null ? List.of() : roles
        );
    }
}
