package com.restaurante.reservas.dto;

import jakarta.validation.constraints.NotNull;

public record MoverReservaRequestDTO(

        @NotNull(message = "La mesa destino es obligatoria")
        Long nuevaMesaId) {
}