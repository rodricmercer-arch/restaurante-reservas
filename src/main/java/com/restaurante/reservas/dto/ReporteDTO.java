package com.restaurante.reservas.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record ReporteDTO(
        Map<String, Long> reservasPorEstado,
        Map<String, Long> mesasPorEstado,
        Map<String, Long> platosPorCategoria,
        List<PlatoTop> topPlatos,
        BigDecimal ingresoEstimado) {

    /** Un plato del ranking: nombre y total de unidades reservadas. */
    public record PlatoTop(String nombre, long unidades) {}

    /** Porcentaje (0-100) de un valor dentro de su grupo; alimenta las barras .progress de la vista. */
    public int porcentaje(long valor, Map<String, Long> grupo) {
        long total = grupo.values().stream().mapToLong(Long::longValue).sum();
        return total == 0 ? 0 : (int) (valor * 100 / total);
    }

    /** Porcentaje (0-100) de un plato respecto al más reservado (el primero del ranking). */
    public int porcentajeTop(long unidades) {
        long max = topPlatos.isEmpty() ? 0 : topPlatos.get(0).unidades();
        return max == 0 ? 0 : (int) (unidades * 100 / max);
    }
}