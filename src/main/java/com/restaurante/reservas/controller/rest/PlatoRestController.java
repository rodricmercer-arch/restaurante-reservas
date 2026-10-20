package com.restaurante.reservas.controller.rest;

import com.restaurante.reservas.dto.PlatoRequestDTO;
import com.restaurante.reservas.dto.PlatoResponseDTO;
import com.restaurante.reservas.model.CategoriaPlato;
import com.restaurante.reservas.model.Plato;
import com.restaurante.reservas.service.CartaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Optional;

/**
 * API REST para la entidad Plato (sección 2.3 del proyecto).
 * Vive de forma aislada en el subpaquete controller.rest y no reemplaza
 * ni modifica el AdminController basado en Thymeleaf: ambos coexisten
 * y usan el mismo CartaService.
 */
@RestController
@RequestMapping("/api/platos")
@Tag(name = "Platos", description = "CRUD de platos de la carta (lectura: CLIENTE y ADMIN; escritura: solo ADMIN)")
@SecurityRequirement(name = "bearerAuth")
public class PlatoRestController {

    private final CartaService cartaService;

    public PlatoRestController(CartaService cartaService) {
        this.cartaService = cartaService;
    }


    // GET /api/platos            -> todos los platos
    // GET /api/platos?categoria=PRINCIPAL -> filtrados por categoría
    @Operation(summary = "Lista los platos, con filtro opcional por categoría")
    @GetMapping
    public ResponseEntity<List<PlatoResponseDTO>> listar(
            @RequestParam(required = false) CategoriaPlato categoria) {

        List<Plato> platos = (categoria != null)
                ? cartaService.buscarPorCategoria(categoria)
                : cartaService.todos();

        List<PlatoResponseDTO> respuesta = platos.stream()
                .map(PlatoResponseDTO::fromEntity)
                .toList();

        return ResponseEntity.ok(respuesta);
    }


    // GET /api/platos/{id}
    @Operation(summary = "Obtiene un plato por su id")
    @GetMapping("/{id}")
    public ResponseEntity<PlatoResponseDTO> obtenerPorId(@PathVariable Long id) {
        return cartaService.buscarPorId(id)
                .map(PlatoResponseDTO::fromEntity)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // POST /api/platos
    @Operation(summary = "Crea un plato (solo ADMIN)")
    @PostMapping
    public ResponseEntity<PlatoResponseDTO> crear(@Valid @RequestBody PlatoRequestDTO request) {
        Plato nuevo = new Plato(
                request.getNombre(),
                request.getDescripcion(),
                request.getPrecio(),
                request.getCategoria()
        );

        Plato guardado = cartaService.guardar(nuevo);
        PlatoResponseDTO respuesta = PlatoResponseDTO.fromEntity(guardado);

        URI location = URI.create("/api/platos/" + guardado.getId());
        return ResponseEntity.created(location).body(respuesta);
    }

    // PUT /api/platos/{id}
    @Operation(summary = "Actualiza un plato existente (solo ADMIN)")
    @PutMapping("/{id}")
    public ResponseEntity<PlatoResponseDTO> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody PlatoRequestDTO request) {

        Optional<Plato> existente = cartaService.buscarPorId(id);
        if (existente.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Plato plato = existente.get();
        plato.setNombre(request.getNombre());
        plato.setDescripcion(request.getDescripcion());
        plato.setPrecio(request.getPrecio());
        plato.setCategoria(request.getCategoria());

        Plato actualizado = cartaService.guardar(plato);
        return ResponseEntity.ok(PlatoResponseDTO.fromEntity(actualizado));
    }

    // DELETE /api/platos/{id}
    @Operation(summary = "Elimina un plato (solo ADMIN); 409 si ya fue usado en reservas")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        Optional<Plato> existente = cartaService.buscarPorId(id);
        if (existente.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        cartaService.eliminar(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
