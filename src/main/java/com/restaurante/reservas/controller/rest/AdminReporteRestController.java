package com.restaurante.reservas.controller.rest;

import com.restaurante.reservas.dto.ReporteDTO;
import com.restaurante.reservas.service.ReporteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Equivale a GET /reportes de ReporteController: mismo servicio, mismo resultado, sin parámetros. */
@RestController
@RequestMapping("/api/admin/reportes")
@Tag(name = "Reportes (admin)", description = "Estadísticas de reservas, mesas y platos; solo ADMIN")
@SecurityRequirement(name = "bearerAuth")
public class AdminReporteRestController {

    private final ReporteService reporteService;

    public AdminReporteRestController(ReporteService reporteService) {
        this.reporteService = reporteService;
    }

    @GetMapping
    @Operation(summary = "Reporte general: reservas y mesas por estado, platos por categoría, top 5 de platos e ingreso estimado")
    public ResponseEntity<ReporteDTO> reporte() {
        return ResponseEntity.ok(reporteService.generar());
    }
}