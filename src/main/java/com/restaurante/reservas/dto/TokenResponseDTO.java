package com.restaurante.reservas.dto;

public record TokenResponseDTO(String token, String tipo, long expiraEnSegundos, String dni, String rol) {

    public TokenResponseDTO(String token, long expiraEnSegundos, String dni, String rol) {
        this(token, "Bearer", expiraEnSegundos, dni, rol);
    }
}