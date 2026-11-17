package com.restaurante.reservas.controller.rest;

import com.restaurante.reservas.dto.MoverReservaRequestDTO;
import com.restaurante.reservas.dto.ReservaResponseDTO;
import com.restaurante.reservas.model.EstadoReserva;
import com.restaurante.reservas.model.Usuario;
import com.restaurante.reservas.repository.UsuarioRepository;
import com.restaurante.reservas.service.ReservaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin/reservas")
@Tag(name = "Reservas (admin)", description = "Consulta, cancelación y traslado de reservas; solo ADMIN")
@SecurityRequirement(name = "bearerAuth")
public class AdminReservaRestController {

    private final ReservaService reservaService;
    private final UsuarioRepository usuarioRepository;

    public AdminReservaRestController(ReservaService reservaService, UsuarioRepository usuarioRepository) {
        this.reservaService = reservaService;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping
    @Operation(summary = "Lista reservas con filtros opcionales: dni, estado y rango de fechas ISO (yyyy-MM-dd)")
    public ResponseEntity<List<ReservaResponseDTO>> listar(
            @RequestParam(required = false) String dni,
            @RequestParam(required = false) EstadoReserva estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        List<ReservaResponseDTO> respuesta = reservaService.buscarConFiltros(dni, estado, desde, hasta).stream()
                .map(ReservaResponseDTO::fromEntity)
                .toList();
        return ResponseEntity.ok(respuesta);
    }

    @PostMapping("/{id}/cancelar")
    @Operation(summary = "Cancela una reserva y libera su mesa; 400 si no existe; 409 si ya está cancelada")
    public ResponseEntity<Void> cancelar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        Usuario admin = usuarioRepository.findByDni(jwt.getSubject())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "El usuario del token ya no existe."));
        reservaService.cancelarReserva(id, admin);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/mover")
    @Operation(summary = "Mueve una reserva a otra mesa libre; 400 si la reserva o la mesa no existen; 409 si está cancelada, ya está en esa mesa, la mesa destino no está libre o hubo una modificación simultánea")
    public ResponseEntity<Void> mover(@PathVariable Long id, @Valid @RequestBody MoverReservaRequestDTO request) {
        reservaService.moverReserva(id, request.nuevaMesaId());
        return ResponseEntity.noContent().build();
    }
}