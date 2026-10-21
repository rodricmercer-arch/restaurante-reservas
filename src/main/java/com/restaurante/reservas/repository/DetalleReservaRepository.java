package com.restaurante.reservas.repository;

import com.restaurante.reservas.model.DetalleReserva;
import org.springframework.data.jpa.repository.JpaRepository;
import com.restaurante.reservas.model.EstadoReserva;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.util.List;

public interface DetalleReservaRepository extends JpaRepository<DetalleReserva, Long> {
    boolean existsByPlatoId(Long platoId);

    // Platos más reservados: filas [nombre, total de unidades], sin contar reservas en el estado excluido.
    @Query("SELECT p.nombre, SUM(d.cantidad) FROM DetalleReserva d " +
           "JOIN d.plato p " +
           "JOIN d.reserva r " +
           "WHERE r.estado <> :excluida " +
           "GROUP BY p.nombre " +
           "ORDER BY SUM(d.cantidad) DESC, p.nombre ASC")
    List<Object[]> platosMasReservadosJPQL(@Param("excluida") EstadoReserva excluida, Pageable pageable);

    // Ingreso estimado: suma de cantidad * precio de los detalles, sin contar reservas en el estado excluido.
    @Query("SELECT COALESCE(SUM(d.cantidad * p.precio), 0) FROM DetalleReserva d " +
           "JOIN d.plato p " +
           "JOIN d.reserva r " +
           "WHERE r.estado <> :excluida")
    BigDecimal ingresoEstimadoJPQL(@Param("excluida") EstadoReserva excluida);
}