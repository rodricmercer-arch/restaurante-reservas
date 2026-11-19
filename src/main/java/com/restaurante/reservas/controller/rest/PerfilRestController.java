package com.restaurante.reservas.controller.rest;

import com.restaurante.reservas.dto.CambiarPasswordRequestDTO;
import com.restaurante.reservas.dto.PerfilResponseDTO;
import com.restaurante.reservas.model.Rol;
import com.restaurante.reservas.model.Usuario;
import com.restaurante.reservas.repository.ReservaRepository;
import com.restaurante.reservas.repository.UsuarioRepository;
import com.restaurante.reservas.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * Perfil del usuario autenticado (CLIENTE o ADMIN). El DNI siempre sale del token,
 * nunca del body: nadie puede ver ni cambiar la clave de otra persona.
 * No existe PUT /api/perfil porque el MVC tampoco permite editar el nombre.
 */
@RestController
@RequestMapping("/api/perfil")
@Tag(name = "Perfil", description = "Perfil y cambio de contraseña del usuario autenticado (CLIENTE y ADMIN)")
@SecurityRequirement(name = "bearerAuth")
public class PerfilRestController {

    private final UsuarioRepository usuarioRepository;
    private final ReservaRepository reservaRepository;
    private final UsuarioService usuarioService;

    public PerfilRestController(UsuarioRepository usuarioRepository,
                                ReservaRepository reservaRepository,
                                UsuarioService usuarioService) {
        this.usuarioRepository = usuarioRepository;
        this.reservaRepository = reservaRepository;
        this.usuarioService = usuarioService;
    }

    @GetMapping
    @Operation(summary = "Devuelve dni, nombre y rol del usuario del token (y totalReservas si es CLIENTE)")
    public ResponseEntity<PerfilResponseDTO> perfil(@AuthenticationPrincipal Jwt jwt) {
        Optional<Usuario> encontrado = usuarioRepository.findByDni(jwt.getSubject());
        if (encontrado.isEmpty()) {
            // Token válido de un usuario que ya fue eliminado.
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Usuario usuario = encontrado.get();

        // Igual que el MVC: solo el CLIENTE tiene reservas propias que contar.
        Long totalReservas = null;
        if (usuario.getRol() == Rol.CLIENTE) {
            totalReservas = reservaRepository.countByClienteId(usuario.getId());
        }
        return ResponseEntity.ok(PerfilResponseDTO.fromEntity(usuario, totalReservas));
    }

    @PutMapping("/password")
    @Operation(summary = "Cambia la contraseña propia; 400 si la actual es incorrecta, la confirmación no coincide o la nueva tiene menos de 8 caracteres")
    public ResponseEntity<Void> cambiarPassword(@AuthenticationPrincipal Jwt jwt,
                                                @Valid @RequestBody CambiarPasswordRequestDTO request) {
        try {
            usuarioService.cambiarPassword(jwt.getSubject(), request.actual(), request.nueva(), request.confirmacion());
        } catch (IllegalStateException e) {
            // El servicio usa IllegalStateException (409 en el handler); aquí son errores de datos del usuario -> 400.
            throw new IllegalArgumentException(e.getMessage(), e);
        }
        return ResponseEntity.noContent().build();
    }
}