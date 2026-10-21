package com.restaurante.reservas.repository;

import com.restaurante.reservas.model.EstadoMesa;
import com.restaurante.reservas.model.Mesa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface MesaRepository extends JpaRepository<Mesa, Long> {
    List<Mesa> findByEstado(EstadoMesa estado);
    List<Mesa> findAllByOrderByNumeroAsc();

    boolean existsByNumero(Integer numero);

    boolean existsByNumeroAndIdNot(Integer numero, Long id);

    // Cuenta mesas por estado (GROUP BY); cada fila es [estado, cantidad].
    @Query("SELECT m.estado, COUNT(m) FROM Mesa m " +
           "GROUP BY m.estado " +
           "ORDER BY m.estado ASC")
    List<Object[]> contarPorEstadoJPQL();
}
