package com.restaurante.reservas.controller.rest;

import com.restaurante.reservas.dto.DashboardDTO;
import com.restaurante.reservas.model.Usuario;
import com.restaurante.reservas.repository.UsuarioRepository;
import com.restaurante.reservas.service.MesaService;
import com.restaurante.reservas.service.ReservaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Equivale a GET /admin/dashboard del AdminController: nombre del admin y totales de mesas y reservas. */
@RestController
@RequestMapping("/api/admin/dashboard")
@Tag(name = "Dashboard (admin)", description = "Resumen para el panel de administración; solo ADMIN")
@SecurityRequirement(name = "bearerAuth")
public class AdminDashboardRestController {

    private final MesaService mesaService;
    private final ReservaService reservaService;
    private final UsuarioRepository usuarioRepository;

    public AdminDashboardRestController(MesaService mesaService, ReservaService reservaService,
                                        UsuarioRepository usuarioRepository) {
        this.mesaService = mesaService;
        this.reservaService = reservaService;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping
    @Operation(summary = "Devuelve el nombre del administrador, el total de mesas y el total de reservas")
    public ResponseEntity<DashboardDTO> dashboard(@AuthenticationPrincipal Jwt jwt) {
        Usuario admin = usuarioRepository.findByDni(jwt.getSubject())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "El usuario del token ya no existe."));
        return ResponseEntity.ok(DashboardDTO.of(admin,
                mesaService.listarTodas().size(),
                reservaService.listarTodas().size()));
    }
}