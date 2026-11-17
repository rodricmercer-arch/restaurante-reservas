package com.restaurante.reservas.dto;

import com.restaurante.reservas.model.Usuario;

/** Respuesta del registro público: nunca incluye la contraseña. */
public record RegistroResponseDTO(String dni, String nombre) {

    public static RegistroResponseDTO fromEntity(Usuario u) {
        return new RegistroResponseDTO(u.getDni(), u.getNombre());
    }
}