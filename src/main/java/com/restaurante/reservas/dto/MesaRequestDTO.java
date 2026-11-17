package com.restaurante.reservas.dto;

import com.restaurante.reservas.model.Mesa;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Datos que el cliente de la API puede enviar para crear o editar una mesa (el estado nunca viaja aquí). */
public record MesaRequestDTO(

        @NotNull(message = "El número de mesa es obligatorio")
        @Min(value = 1, message = "El número de mesa debe ser mayor o igual a 1")
        Integer numero,

        @NotNull(message = "La capacidad es obligatoria")
        @Min(value = 1, message = "La capacidad mínima es 1 persona")
        @Max(value = 20, message = "La capacidad máxima es 20 personas")
        Integer capacidad) {

    /** Mesa transitoria para pasarla a MesaService.crear/actualizar (el servicio decide id y estado). */
    public Mesa toEntity() {
        return new Mesa(numero, capacidad);
    }
}