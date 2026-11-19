package com.restaurante.reservas.dto;

import com.restaurante.reservas.model.Rol;
import com.restaurante.reservas.model.Usuario;

public record UsuarioResponseDTO(Long id, String dni, String nombre, Rol rol) {

    public static UsuarioResponseDTO fromEntity(Usuario u) {
        return new UsuarioResponseDTO(u.getId(), u.getDni(), u.getNombre(), u.getRol());
    }
}