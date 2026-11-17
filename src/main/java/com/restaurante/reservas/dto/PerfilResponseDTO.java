package com.restaurante.reservas.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.restaurante.reservas.model.Rol;
import com.restaurante.reservas.model.Usuario;

/** Perfil del usuario autenticado. totalReservas solo aplica a CLIENTE; para ADMIN se omite del JSON. */
public record PerfilResponseDTO(
        String dni,
        String nombre,
        Rol rol,
        @JsonInclude(JsonInclude.Include.NON_NULL) Long totalReservas) {

    public static PerfilResponseDTO fromEntity(Usuario u, Long totalReservas) {
        return new PerfilResponseDTO(u.getDni(), u.getNombre(), u.getRol(), totalReservas);
    }
}