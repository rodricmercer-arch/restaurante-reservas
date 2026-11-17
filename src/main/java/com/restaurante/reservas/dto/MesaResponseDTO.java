package com.restaurante.reservas.dto;

import com.restaurante.reservas.model.EstadoMesa;
import com.restaurante.reservas.model.Mesa;

public record MesaResponseDTO(Long id, Integer numero, Integer capacidad, EstadoMesa estado) {

    public static MesaResponseDTO fromEntity(Mesa mesa) {
        return new MesaResponseDTO(mesa.getId(), mesa.getNumero(), mesa.getCapacidad(), mesa.getEstado());
    }
}