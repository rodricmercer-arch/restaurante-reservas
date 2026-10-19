package com.restaurante.reservas.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequestDTO(
        @NotBlank(message = "El DNI es obligatorio") String dni,
        @NotBlank(message = "La contraseña es obligatoria") String password) {
}