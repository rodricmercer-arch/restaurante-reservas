package com.restaurante.reservas.dto;

import com.restaurante.reservas.model.Usuario;

public record DashboardDTO(String nombre, int totalMesas, int totalReservas) {

    public static DashboardDTO of(Usuario admin, int totalMesas, int totalReservas) {
        return new DashboardDTO(admin.getNombre(), totalMesas, totalReservas);
    }
}