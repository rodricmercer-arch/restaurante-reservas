package com.restaurante.reservas.controller.rest;

import com.restaurante.reservas.dto.CrearReservaRequestDTO;
import com.restaurante.reservas.dto.ReservaResponseDTO;
import com.restaurante.reservas.model.Reserva;
import com.restaurante.reservas.model.Usuario;
import com.restaurante.reservas.repository.UsuarioRepository;
import com.restaurante.reservas.service.NotificacionService;
import com.restaurante.reservas.service.ReservaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;

/** Equivale a ClienteController (MVC): mismos servicios, mismas reglas; el usuario sale del token (subject = DNI). */
@RestController
@RequestMapping("/api/cliente")
@Tag(name = "Cliente", description = "Reserva de mesa del cliente autenticado (solo CLIENTE)")
@SecurityRequirement(name = "bearerAuth")
public class ClienteRestController {

    private final ReservaService reservaService;
    private final NotificacionService notificacionService;
    private final UsuarioRepository usuarioRepository;

    public ClienteRestController(ReservaService reservaService,
                                 NotificacionService notificacionService,
                                 UsuarioRepository usuarioRepository) {
        this.reservaService = reservaService;
        this.notificacionService = notificacionService;
        this.usuarioRepository = usuarioRepository;
    }

    private Usuario usuarioActual(Jwt jwt) {
        return usuarioRepository.findByDni(jwt.getSubject())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "El usuario del token ya no existe."));
    }

    @GetMapping("/reserva-activa")
    @Operation(summary = "Devuelve la reserva activa del cliente (200) o 204 si no tiene")
    public ResponseEntity<ReservaResponseDTO> reservaActiva(@AuthenticationPrincipal Jwt jwt) {
        return reservaService.buscarReservaActiva(usuarioActual(jwt))
                .map(ReservaResponseDTO::fromEntity)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/reservas")
    @Operation(summary = "Crea una reserva; 409 si ya tiene una activa o la mesa no está disponible; 400 si la mesa o un plato no existen")
    public ResponseEntity<ReservaResponseDTO> crear(@AuthenticationPrincipal Jwt jwt,
                                                    @Valid @RequestBody CrearReservaRequestDTO request) {
        Usuario cliente = usuarioActual(jwt);
        Reserva creada = reservaService.crearReserva(cliente, request.mesaId(), request.platoPrincipalId(),
                request.platoPostreId(), request.platoBebidaId());
        // Igual que ClienteController: aviso por Telegram solo si la reserva se guardó; nunca lanza excepciones.
        notificacionService.notificarReservaNueva(creada);

        URI location = URI.create("/api/cliente/reserva-activa");
        return ResponseEntity.created(location).body(ReservaResponseDTO.fromEntity(creada));
    }

    @PostMapping("/reservas/{id}/cancelar")
    @Operation(summary = "Cancela una reserva propia; 400 si no existe; 409 si no es tuya o ya está cancelada")
    public ResponseEntity<Void> cancelar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        reservaService.cancelarReserva(id, usuarioActual(jwt));
        return ResponseEntity.noContent().build();
    }
}