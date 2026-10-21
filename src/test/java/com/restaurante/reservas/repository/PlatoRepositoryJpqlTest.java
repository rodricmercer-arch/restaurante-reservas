package com.restaurante.reservas.repository;

import com.restaurante.reservas.model.CategoriaPlato;
import com.restaurante.reservas.model.Plato;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Prueba las consultas JPQL de PlatoRepository contra H2 en memoria. */
@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class PlatoRepositoryJpqlTest {

    // Valores "neutros" que el servicio usa cuando un filtro viene vacío.
    private static final BigDecimal PRECIO_MIN = BigDecimal.ZERO;
    private static final BigDecimal PRECIO_MAX = new BigDecimal("99999999.99");
    private static final List<CategoriaPlato> TODAS = List.of(CategoriaPlato.values());

    @Autowired
    private TestEntityManager em;

    @Autowired
    private PlatoRepository platoRepository;

    @BeforeEach
    void datos() {
        persistir("Lomo Saltado", "Clásico peruano", "32.00", CategoriaPlato.PRINCIPAL);
        persistir("Arroz con Pollo", "Arroz verde", "28.00", CategoriaPlato.PRINCIPAL);
        persistir("Suspiro a la Limeña", "Postre tradicional", "12.00", CategoriaPlato.POSTRE);
        persistir("Chicha Morada", "Bebida tradicional", "8.00", CategoriaPlato.BEBIDA);
        persistir("Gaseosa", "500 ml", "6.00", CategoriaPlato.BEBIDA);
        em.flush();
        em.clear();
    }

    private void persistir(String nombre, String descripcion, String precio, CategoriaPlato categoria) {
        em.persist(new Plato(nombre, descripcion, new BigDecimal(precio), categoria));
    }

    @Test
    void buscarAvanzado_sinFiltros_devuelveTodosOrdenadosPorCategoriaYNombre() {
        List<Plato> r = platoRepository.buscarAvanzadoJPQL("", TODAS, PRECIO_MIN, PRECIO_MAX);

        assertThat(r).extracting(Plato::getNombre).containsExactly(
                "Chicha Morada", "Gaseosa",          // BEBIDA
                "Suspiro a la Limeña",               // POSTRE
                "Arroz con Pollo", "Lomo Saltado");  // PRINCIPAL
    }

    @Test
    void buscarAvanzado_porTexto_ignoraMayusculas() {
        List<Plato> r = platoRepository.buscarAvanzadoJPQL("LOMO", TODAS, PRECIO_MIN, PRECIO_MAX);

        assertThat(r).extracting(Plato::getNombre).containsExactly("Lomo Saltado");
    }

    @Test
    void buscarAvanzado_porCategoria_filtraConIn() {
        List<Plato> r = platoRepository.buscarAvanzadoJPQL(
                "", List.of(CategoriaPlato.BEBIDA), PRECIO_MIN, PRECIO_MAX);

        assertThat(r).extracting(Plato::getNombre).containsExactly("Chicha Morada", "Gaseosa");
    }

    @Test
    void buscarAvanzado_porRangoDePrecio_filtraConBetween() {
        List<Plato> r = platoRepository.buscarAvanzadoJPQL(
                "", TODAS, new BigDecimal("7.00"), new BigDecimal("30.00"));

        assertThat(r).extracting(Plato::getNombre)
                .containsExactly("Chicha Morada", "Suspiro a la Limeña", "Arroz con Pollo");
    }

    @Test
    void buscarAvanzado_conTodosLosFiltrosCombinados() {
        List<Plato> r = platoRepository.buscarAvanzadoJPQL(
                "o", List.of(CategoriaPlato.PRINCIPAL), new BigDecimal("30.00"), new BigDecimal("40.00"));

        assertThat(r).extracting(Plato::getNombre).containsExactly("Lomo Saltado");
    }

    @Test
    void buscarAvanzado_sinCoincidencias_devuelveListaVacia() {
        List<Plato> r = platoRepository.buscarAvanzadoJPQL("pizza", TODAS, PRECIO_MIN, PRECIO_MAX);

        assertThat(r).isEmpty();
    }

    @Test
    void contarPorCategoria_agrupaYCuenta() {
        Map<CategoriaPlato, Long> conteo = new HashMap<>();
        for (Object[] fila : platoRepository.contarPorCategoriaJPQL()) {
            conteo.put((CategoriaPlato) fila[0], ((Number) fila[1]).longValue());
        }

        assertThat(conteo)
                .containsEntry(CategoriaPlato.PRINCIPAL, 2L)
                .containsEntry(CategoriaPlato.POSTRE, 1L)
                .containsEntry(CategoriaPlato.BEBIDA, 2L);
    }

    @Test
    void buscarPorNombre_listarOrdenados_yRangoPrecio() {
        assertThat(platoRepository.buscarPorNombreJPQL("POLLO"))
                .extracting(Plato::getNombre).containsExactly("Arroz con Pollo");

        assertThat(platoRepository.buscarPorRangoPrecioJPQL(new BigDecimal("10.00"), new BigDecimal("30.00")))
                .extracting(Plato::getNombre).containsExactly("Suspiro a la Limeña", "Arroz con Pollo");

        assertThat(platoRepository.listarOrdenadosJPQL().get(0).getNombre()).isEqualTo("Chicha Morada");
    }
}