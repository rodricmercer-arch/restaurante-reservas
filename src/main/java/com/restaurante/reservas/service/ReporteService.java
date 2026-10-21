package com.restaurante.reservas.service;

import com.restaurante.reservas.dto.ReporteDTO;
import com.restaurante.reservas.model.EstadoReserva;
import com.restaurante.reservas.repository.DetalleReservaRepository;
import com.restaurante.reservas.repository.MesaRepository;
import com.restaurante.reservas.repository.PlatoRepository;
import com.restaurante.reservas.repository.ReservaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
// Solo lectura: el reporte únicamente consulta, así Hibernate no hace flush y la BD puede optimizar.
@Transactional(readOnly = true)
public class ReporteService {

    private final ReservaRepository reservaRepository;
    private final MesaRepository mesaRepository;
    private final PlatoRepository platoRepository;
    private final DetalleReservaRepository detalleReservaRepository;

    public ReporteService(ReservaRepository reservaRepository,
                          MesaRepository mesaRepository,
                          PlatoRepository platoRepository,
                          DetalleReservaRepository detalleReservaRepository) {
        this.reservaRepository = reservaRepository;
        this.mesaRepository = mesaRepository;
        this.platoRepository = platoRepository;
        this.detalleReservaRepository = detalleReservaRepository;
    }

    @PreAuthorize("hasRole('ADMIN')")
    public ReporteDTO generar() {
        Map<String, Long> reservasPorEstado = aMapa(reservaRepository.contarPorEstadoJPQL());
        Map<String, Long> mesasPorEstado = aMapa(mesaRepository.contarPorEstadoJPQL());
        Map<String, Long> platosPorCategoria = aMapa(platoRepository.contarPorCategoriaJPQL());

        // Top 5 platos; las reservas CANCELADAS no cuentan como consumo.
        List<ReporteDTO.PlatoTop> top = new ArrayList<>();
        List<Object[]> filas = detalleReservaRepository
                .platosMasReservadosJPQL(EstadoReserva.CANCELADA, PageRequest.of(0, 5));
        for (Object[] fila : filas) {
            top.add(new ReporteDTO.PlatoTop((String) fila[0], ((Number) fila[1]).longValue()));
        }

        BigDecimal ingreso = detalleReservaRepository.ingresoEstimadoJPQL(EstadoReserva.CANCELADA);

        return new ReporteDTO(reservasPorEstado, mesasPorEstado, platosPorCategoria, top,
                ingreso == null ? BigDecimal.ZERO : ingreso);
    }

    // Convierte filas [clave, cantidad] en un mapa que conserva el orden de la consulta.
    private Map<String, Long> aMapa(List<Object[]> filas) {
        Map<String, Long> mapa = new LinkedHashMap<>();
        for (Object[] fila : filas) {
            mapa.put(String.valueOf(fila[0]), ((Number) fila[1]).longValue());
        }
        return mapa;
    }
}