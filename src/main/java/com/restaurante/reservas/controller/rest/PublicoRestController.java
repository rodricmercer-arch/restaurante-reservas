package com.restaurante.reservas.controller.rest;

import com.restaurante.reservas.dto.MesaResponseDTO;
import com.restaurante.reservas.dto.PlatoResponseDTO;
import com.restaurante.reservas.model.CategoriaPlato;
import com.restaurante.reservas.model.Plato;
import com.restaurante.reservas.service.CartaService;
import com.restaurante.reservas.service.MesaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Carta y mesas visibles sin iniciar sesión (equivale a "/" y "/carta" del MVC). Sin SecurityRequirement. */
@RestController
@RequestMapping("/api/publico")
@Tag(name = "Público", description = "Carta y mesas visibles sin token")
public class PublicoRestController {

    private final CartaService cartaService;
    private final MesaService mesaService;

    public PublicoRestController(CartaService cartaService, MesaService mesaService) {
        this.cartaService = cartaService;
        this.mesaService = mesaService;
    }

    @GetMapping("/carta")
    @Operation(summary = "Lista la carta, con filtro opcional por categoría (sin token)")
    public ResponseEntity<List<PlatoResponseDTO>> carta(
            @RequestParam(required = false) CategoriaPlato categoria) {
        List<Plato> platos = (categoria != null)
                ? cartaService.buscarPorCategoria(categoria)
                : cartaService.todos();
        return ResponseEntity.ok(platos.stream().map(PlatoResponseDTO::fromEntity).toList());
    }

    @GetMapping("/mesas")
    @Operation(summary = "Lista las mesas ordenadas por número (sin token)")
    public ResponseEntity<List<MesaResponseDTO>> mesas() {
        // listarTodas() ya viene ordenada por número (findAllByOrderByNumeroAsc).
        return ResponseEntity.ok(mesaService.listarTodas().stream().map(MesaResponseDTO::fromEntity).toList());
    }
}