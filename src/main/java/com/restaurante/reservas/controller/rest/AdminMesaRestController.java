package com.restaurante.reservas.controller.rest;

import com.restaurante.reservas.dto.MesaRequestDTO;
import com.restaurante.reservas.dto.MesaResponseDTO;
import com.restaurante.reservas.model.Mesa;
import com.restaurante.reservas.service.MesaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/admin/mesas")
@Tag(name = "Mesas (admin)", description = "CRUD de mesas; solo ADMIN")
@SecurityRequirement(name = "bearerAuth")
public class AdminMesaRestController {

    private final MesaService mesaService;

    public AdminMesaRestController(MesaService mesaService) {
        this.mesaService = mesaService;
    }

    @GetMapping
    @Operation(summary = "Lista todas las mesas ordenadas por número")
    public ResponseEntity<List<MesaResponseDTO>> listar() {
        return ResponseEntity.ok(mesaService.listarTodas().stream().map(MesaResponseDTO::fromEntity).toList());
    }

    @PostMapping
    @Operation(summary = "Crea una mesa (nace LIBRE); 409 si el número ya existe")
    public ResponseEntity<MesaResponseDTO> crear(@Valid @RequestBody MesaRequestDTO request) {
        Mesa creada = mesaService.crear(request.toEntity());
        URI location = URI.create("/api/admin/mesas/" + creada.getId());
        return ResponseEntity.created(location).body(MesaResponseDTO.fromEntity(creada));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Edita número y capacidad (el estado no se toca); 404 si no existe; 409 si el número ya existe")
    public ResponseEntity<MesaResponseDTO> actualizar(@PathVariable Long id,
                                                      @Valid @RequestBody MesaRequestDTO request) {
        if (mesaService.buscarPorId(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Mesa actualizada = mesaService.actualizar(id, request.toEntity());
        return ResponseEntity.ok(MesaResponseDTO.fromEntity(actualizada));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Elimina una mesa; 404 si no existe; 409 si tiene reservas registradas")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        if (mesaService.buscarPorId(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        mesaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}