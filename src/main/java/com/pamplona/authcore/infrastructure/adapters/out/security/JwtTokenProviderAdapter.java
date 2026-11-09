package com.pamplona.authcore.infrastructure.adapters.out.security;

import com.pamplona.authcore.application.ports.out.TokenProviderPort;
import com.pamplona.authcore.domain.Role;
import com.pamplona.authcore.domain.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;

/**
 * Adaptador de salida: genera el JWT firmado con la clave compartida.
 * La clave llega en Base64 (JWT_SECRET_BASE64, minimo 48 bytes => HS384).
 * domain-service debe usar EXACTAMENTE la misma clave (authcore.jwt.secret-base64)
 * para validar el token sin llamar a authcore-service en cada peticion.
 */
@Component
public class JwtTokenProviderAdapter implements TokenProviderPort {

    private final SecretKey key;
    private final long expirationMs;

    public JwtTokenProviderAdapter(
            @Value("${authcore.jwt.secret-base64}") String secretBase64,
            @Value("${authcore.jwt.expiration-ms:3600000}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretBase64.trim()));
        this.expirationMs = expirationMs;
    }

    @Override
    public String generateToken(User user) {
        Instant now = Instant.now();
        List<String> roles = user.getRoles().stream().map(Role::name).toList();

        return Jwts.builder()
                .subject(user.getUsername())
                .claim("uid", user.getId())
                .claim("roles", roles)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMs)))
                .signWith(key)
                .compact();
    }
}
