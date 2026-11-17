package com.restaurante.reservas.dto;

import jakarta.validation.constraints.NotNull;

public record CrearReservaRequestDTO(

        @NotNull(message = "La mesa es obligatoria")
        Long mesaId,

        @NotNull(message = "El plato principal es obligatorio")
        Long platoPrincipalId,

        Long platoPostreId,

        Long platoBebidaId) {
}