package com.restaurante.reservas.controller.rest;

import com.restaurante.reservas.dto.LoginRequestDTO;
import com.restaurante.reservas.dto.TokenResponseDTO;
import com.restaurante.reservas.service.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticación", description = "Login por DNI y contraseña para obtener un token JWT")
public class AuthRestController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthRestController(AuthenticationManager authenticationManager, JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    @Operation(summary = "Inicia sesión con DNI y contraseña y devuelve un token JWT")
    public ResponseEntity<TokenResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.dni(), request.password()));

        String rol = auth.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .orElse("");

        String token = jwtService.generarToken(auth);
        return ResponseEntity.ok(new TokenResponseDTO(token, jwtService.getExpiracionSegundos(), auth.getName(), rol));
    }

    @GetMapping("/me")
    @Operation(summary = "Devuelve el DNI y los roles contenidos en el token JWT")
    @SecurityRequirement(name = "bearerAuth")
    public Map<String, Object> me(@AuthenticationPrincipal Jwt jwt) {
        return Map.of("dni", jwt.getSubject(), "roles", jwt.getClaimAsStringList("roles"));
    }
}