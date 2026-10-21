package com.restaurante.reservas.service;

import com.restaurante.reservas.config.JwtConfig;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Prueba unitaria pura (sin Spring): firma un token y lo vuelve a decodificar. */
class JwtServiceTest {

    private static final String SECRETO = "clave-de-pruebas-reservas-restaurante-minimo-32-caracteres";

    @Test
    void tokenGenerado_sePuedeDecodificar_yContieneSubYRoles() {
        JwtConfig config = new JwtConfig(SECRETO);
        JwtService jwtService = new JwtService(config.jwtEncoder(), 60, "reservas-restaurante");

        Authentication auth = new UsernamePasswordAuthenticationToken(
                "00000000", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        String token = jwtService.generarToken(auth);
        Jwt jwt = config.jwtDecoder().decode(token);

        assertThat(jwt.getSubject()).isEqualTo("00000000");
        assertThat(jwt.getClaimAsStringList("roles")).containsExactly("ADMIN");
        assertThat(jwt.getClaimAsString("iss")).isEqualTo("reservas-restaurante");
        assertThat(jwt.getExpiresAt()).isAfter(jwt.getIssuedAt());
        assertThat(jwtService.getExpiracionSegundos()).isEqualTo(3600);
    }
}