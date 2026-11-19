package com.restaurante.reservas.dto;

import jakarta.validation.constraints.NotBlank;

public record CambiarPasswordRequestDTO(

        @NotBlank(message = "La contraseña actual es obligatoria")
        String actual,

        @NotBlank(message = "La nueva contraseña es obligatoria")
        String nueva,

        @NotBlank(message = "Debes confirmar la nueva contraseña")
        String confirmacion) {
}