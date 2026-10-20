package com.restaurante.reservas.repository;

import com.restaurante.reservas.model.CategoriaPlato;
import com.restaurante.reservas.model.Plato;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.util.List;

public interface PlatoRepository extends JpaRepository<Plato, Long> {
    List<Plato> findByCategoria(CategoriaPlato categoria);

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);

    // Lista todos los platos ordenados por categoría y luego por nombre.
    @Query("SELECT p FROM Plato p " +
           "ORDER BY p.categoria ASC, p.nombre ASC")
    List<Plato> listarOrdenadosJPQL();

    // Busca platos cuyo nombre contenga el texto, sin distinguir mayúsculas (LOWER + LIKE + CONCAT).
    @Query("SELECT p FROM Plato p " +
           "WHERE LOWER(p.nombre) LIKE LOWER(CONCAT('%', :nombre, '%')) " +
           "ORDER BY p.nombre ASC")
    List<Plato> buscarPorNombreJPQL(@Param("nombre") String nombre);

    // Platos cuyo precio está dentro de un rango mínimo-máximo (BETWEEN).
    @Query("SELECT p FROM Plato p " +
           "WHERE p.precio BETWEEN :min AND :max " +
           "ORDER BY p.precio ASC")
    List<Plato> buscarPorRangoPrecioJPQL(@Param("min") BigDecimal min,
                                         @Param("max") BigDecimal max);

    // Búsqueda avanzada: texto en el nombre, categorías (IN) y rango de precio (BETWEEN); el servicio normaliza los nulos.
    @Query("SELECT p FROM Plato p " +
           "WHERE LOWER(p.nombre) LIKE LOWER(CONCAT('%', :texto, '%')) " +
           "AND p.categoria IN :categorias " +
           "AND p.precio BETWEEN :precioMin AND :precioMax " +
           "ORDER BY p.categoria ASC, p.nombre ASC")
    List<Plato> buscarAvanzadoJPQL(@Param("texto") String texto,
                                   @Param("categorias") List<CategoriaPlato> categorias,
                                   @Param("precioMin") BigDecimal precioMin,
                                   @Param("precioMax") BigDecimal precioMax);

    // Cuenta platos por categoría (GROUP BY); cada fila es [categoria, cantidad].
    @Query("SELECT p.categoria, COUNT(p) FROM Plato p " +
           "GROUP BY p.categoria " +
           "ORDER BY p.categoria ASC")
    List<Object[]> contarPorCategoriaJPQL();
}
