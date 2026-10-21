package com.restaurante.reservas.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final long expiracionMinutos;
    private final String emisor;

    public JwtService(JwtEncoder jwtEncoder,
                      @Value("${app.jwt.expiration-minutes}") long expiracionMinutos,
                      @Value("${app.jwt.issuer}") String emisor) {
        this.jwtEncoder = jwtEncoder;
        this.expiracionMinutos = expiracionMinutos;
        this.emisor = emisor;
    }

    /** Genera un JWT con sub=dni, roles=["ADMIN"|"CLIENTE"], iss, iat y exp. */
    public String generarToken(Authentication authentication) {
        Instant ahora = Instant.now();

        // Guardamos los roles SIN el prefijo ROLE_ (el converter lo vuelve a agregar).
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(a -> a.startsWith("ROLE_") ? a.substring(5) : a)
                .toList();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(emisor)
                .issuedAt(ahora)
                .expiresAt(ahora.plus(expiracionMinutos, ChronoUnit.MINUTES))
                .subject(authentication.getName())
                .claim("roles", roles)
                .build();

        // El header debe indicar HS256 explícitamente; si no, Nimbus intenta RS256 y falla.
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public long getExpiracionSegundos() {
        return expiracionMinutos * 60;
    }
}