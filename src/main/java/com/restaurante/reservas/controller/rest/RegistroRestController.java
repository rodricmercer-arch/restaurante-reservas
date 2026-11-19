package com.restaurante.reservas.controller.rest;

import com.restaurante.reservas.dto.RegistroFormDTO;
import com.restaurante.reservas.dto.RegistroResponseDTO;
import com.restaurante.reservas.model.Usuario;
import com.restaurante.reservas.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Registro público (sin token). Equivale a POST /registro de RegistroController:
 * el rol siempre es CLIENTE y lo fija UsuarioService, nunca el body.
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticación", description = "Login por DNI y contraseña para obtener un token JWT; registro público de clientes")
public class RegistroRestController {

    private final UsuarioService usuarioService;

    public RegistroRestController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping("/registro")
    @Operation(summary = "Registra un cliente nuevo (rol siempre CLIENTE); 409 si el DNI ya existe o la confirmación no coincide")
    public ResponseEntity<RegistroResponseDTO> registrar(@Valid @RequestBody RegistroFormDTO request) {
        Usuario creado = usuarioService.registrarCliente(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(RegistroResponseDTO.fromEntity(creado));
    }
}