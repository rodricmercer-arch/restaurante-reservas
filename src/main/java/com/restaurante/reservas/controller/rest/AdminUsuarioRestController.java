package com.restaurante.reservas.controller.rest;

import com.restaurante.reservas.dto.UsuarioRequestDTO;
import com.restaurante.reservas.dto.UsuarioResponseDTO;
import com.restaurante.reservas.model.Usuario;
import com.restaurante.reservas.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

/**
 * Gestión de usuarios (solo ADMIN). Reutiliza UsuarioService, que ya aplica las reglas del MVC:
 * DNI único, contraseña mínima de 8, no degradar ni eliminar al último administrador,
 * no eliminarse a sí mismo y no eliminar usuarios con reservas (todas -> 409).
 */
@RestController
@RequestMapping("/api/admin/usuarios")
@Tag(name = "Usuarios (admin)", description = "CRUD de usuarios; solo ADMIN")
@SecurityRequirement(name = "bearerAuth")
public class AdminUsuarioRestController {

    private final UsuarioService usuarioService;

    public AdminUsuarioRestController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    @Operation(summary = "Lista todos los usuarios ordenados por nombre")
    public ResponseEntity<List<UsuarioResponseDTO>> listar() {
        List<UsuarioResponseDTO> respuesta = usuarioService.listar().stream()
                .map(UsuarioResponseDTO::fromEntity)
                .toList();
        return ResponseEntity.ok(respuesta);
    }

    @PostMapping
    @Operation(summary = "Crea un usuario (la contraseña es obligatoria); 409 si el DNI ya existe")
    public ResponseEntity<UsuarioResponseDTO> crear(@Valid @RequestBody UsuarioRequestDTO request) {
        Usuario creado = usuarioService.crear(request.toForm(null));
        URI location = URI.create("/api/admin/usuarios/" + creado.getId());
        return ResponseEntity.created(location).body(UsuarioResponseDTO.fromEntity(creado));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Edita nombre y rol; la contraseña solo cambia si se envía (el DNI no se modifica); 404 si no existe; 409 si deja al sistema sin administradores")
    public ResponseEntity<UsuarioResponseDTO> actualizar(@PathVariable Long id,
                                                         @Valid @RequestBody UsuarioRequestDTO request) {
        if (usuarioService.buscarPorId(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Usuario actualizado = usuarioService.actualizar(request.toForm(id));
        return ResponseEntity.ok(UsuarioResponseDTO.fromEntity(actualizado));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Elimina un usuario; 404 si no existe; 409 si es tu propia cuenta, el último administrador o tiene reservas")
    public ResponseEntity<Void> eliminar(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        if (usuarioService.buscarPorId(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        // El DNI del admin sale del token (subject), igual que auth.getName() en el MVC.
        usuarioService.eliminar(id, jwt.getSubject());
        return ResponseEntity.noContent().build();
    }
}