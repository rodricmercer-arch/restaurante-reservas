package com.restaurante.reservas.repository;

import com.restaurante.reservas.model.EstadoReserva;
import com.restaurante.reservas.model.Reserva;
import com.restaurante.reservas.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    Optional<Reserva> findFirstByClienteAndEstadoIn(Usuario cliente, List<EstadoReserva> estados);

    List<Reserva> findByClienteOrderByFechaHoraDesc(Usuario cliente);

    List<Reserva> findAllByOrderByFechaHoraDesc();

    boolean existsByMesaId(Long mesaId);

    boolean existsByClienteId(Long clienteId);

    long countByClienteId(Long clienteId);

    // Filtra reservas por DNI (LIKE), estados (IN) y rango de fechas (BETWEEN), trayendo cliente y mesa con JOIN FETCH.
    @Query("SELECT r FROM Reserva r " +
           "JOIN FETCH r.cliente c " +
           "JOIN FETCH r.mesa m " +
           "WHERE c.dni LIKE CONCAT('%', :dni, '%') " +
           "AND r.estado IN :estados " +
           "AND r.fechaHora BETWEEN :desde AND :hasta " +
           "ORDER BY r.fechaHora DESC")
    List<Reserva> buscarConFiltrosJPQL(@Param("dni") String dni,
                                       @Param("estados") List<EstadoReserva> estados,
                                       @Param("desde") LocalDateTime desde,
                                       @Param("hasta") LocalDateTime hasta);

    // Cuenta reservas por estado (GROUP BY); cada fila es [estado, cantidad].
    @Query("SELECT r.estado, COUNT(r) FROM Reserva r " +
           "GROUP BY r.estado " +
           "ORDER BY r.estado ASC")
    List<Object[]> contarPorEstadoJPQL();
}
