package com.restaurante.reservas.repository;

import com.restaurante.reservas.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Prueba las consultas JPQL de ReservaRepository y DetalleReservaRepository contra H2 en memoria. */
@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class ReservaRepositoryJpqlTest {

    // Valores "neutros" que el servicio usa cuando un filtro viene vacío.
    private static final LocalDateTime DESDE_ABIERTO = LocalDateTime.of(2000, 1, 1, 0, 0);
    private static final LocalDateTime HASTA_ABIERTO = LocalDateTime.of(2999, 12, 31, 23, 59, 59, 999_999_999);
    private static final List<EstadoReserva> TODOS = List.of(EstadoReserva.values());

    @Autowired
    private TestEntityManager em;

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private DetalleReservaRepository detalleReservaRepository;

    /*
     * Datos:
     *  r1: Ana  (12345678), mesa 1, CONFIRMADA, 01/10 12:00 -> Lomo x2, Chicha x1
     *  r2: Beto (87654321), mesa 2, PENDIENTE,  05/10 20:00 -> Lomo x1
     *  r3: Beto (87654321), mesa 3, CANCELADA,  10/10 13:00 -> Chicha x5
     */
    @BeforeEach
    void datos() {
        Usuario ana = em.persist(new Usuario("12345678", "x", "Ana", Rol.CLIENTE));
        Usuario beto = em.persist(new Usuario("87654321", "x", "Beto", Rol.CLIENTE));

        Mesa m1 = em.persist(new Mesa(1, 4));
        Mesa m2 = em.persist(new Mesa(2, 4));
        Mesa m3 = em.persist(new Mesa(3, 2));

        Plato lomo = em.persist(new Plato("Lomo Saltado", "Clásico", new BigDecimal("30.00"), CategoriaPlato.PRINCIPAL));
        Plato chicha = em.persist(new Plato("Chicha Morada", "Bebida", new BigDecimal("10.00"), CategoriaPlato.BEBIDA));

        crearReserva(ana, m1, EstadoReserva.CONFIRMADA, LocalDateTime.of(2026, 10, 1, 12, 0),
                new DetalleReserva(lomo, 2), new DetalleReserva(chicha, 1));
        crearReserva(beto, m2, EstadoReserva.PENDIENTE, LocalDateTime.of(2026, 10, 5, 20, 0),
                new DetalleReserva(lomo, 1));
        crearReserva(beto, m3, EstadoReserva.CANCELADA, LocalDateTime.of(2026, 10, 10, 13, 0),
                new DetalleReserva(chicha, 5));

        em.flush();
        em.clear();
    }

    private void crearReserva(Usuario cliente, Mesa mesa, EstadoReserva estado,
                              LocalDateTime fecha, DetalleReserva... detalles) {
        Reserva r = new Reserva();
        r.setCliente(cliente);
        r.setMesa(mesa);
        r.setEstado(estado);
        r.setFechaHora(fecha);
        for (DetalleReserva d : detalles) {
            r.addDetalle(d); // cascade ALL: se guardan junto con la reserva
        }
        em.persist(r);
    }

    private List<Reserva> buscar(String dni, List<EstadoReserva> estados, LocalDateTime desde, LocalDateTime hasta) {
        return reservaRepository.buscarConFiltrosJPQL(dni, estados, desde, hasta);
    }

    // ---------- buscarConFiltrosJPQL ----------

    @Test
    void buscarConFiltros_sinFiltros_devuelveTodasOrdenadasPorFechaDescendente() {
        List<Reserva> r = buscar("", TODOS, DESDE_ABIERTO, HASTA_ABIERTO);

        assertThat(r).extracting(x -> x.getMesa().getNumero()).containsExactly(3, 2, 1);
    }

    @Test
    void buscarConFiltros_porEstado() {
        assertThat(buscar("", List.of(EstadoReserva.CANCELADA), DESDE_ABIERTO, HASTA_ABIERTO))
                .extracting(x -> x.getMesa().getNumero()).containsExactly(3);

        assertThat(buscar("", List.of(EstadoReserva.PENDIENTE, EstadoReserva.CONFIRMADA), DESDE_ABIERTO, HASTA_ABIERTO))
                .extracting(x -> x.getMesa().getNumero()).containsExactly(2, 1);
    }

    @Test
    void buscarConFiltros_porDniParcial() {
        List<Reserva> ana = buscar("1234", TODOS, DESDE_ABIERTO, HASTA_ABIERTO);
        assertThat(ana).hasSize(1);
        assertThat(ana.get(0).getCliente().getDni()).isEqualTo("12345678");

        assertThat(buscar("8765", TODOS, DESDE_ABIERTO, HASTA_ABIERTO)).hasSize(2);
    }

    @Test
    void buscarConFiltros_dniMasEstado() {
        assertThat(buscar("8765", List.of(EstadoReserva.CANCELADA), DESDE_ABIERTO, HASTA_ABIERTO))
                .extracting(x -> x.getMesa().getNumero()).containsExactly(3);
    }

    @Test
    void buscarConFiltros_porRangoDeFechas() {
        List<Reserva> r = buscar("", TODOS,
                LocalDateTime.of(2026, 10, 1, 0, 0), LocalDateTime.of(2026, 10, 6, 0, 0));

        assertThat(r).extracting(x -> x.getMesa().getNumero()).containsExactly(2, 1);
    }

    @Test
    void buscarConFiltros_hastaIncluyeTodoElDia() {
        // Igual que hace el servicio: "hasta" = último instante de ese día.
        LocalDateTime desde = LocalDate.of(2026, 10, 5).atStartOfDay();
        LocalDateTime hasta = LocalDate.of(2026, 10, 5).atTime(LocalTime.MAX);

        assertThat(buscar("", TODOS, desde, hasta))
                .extracting(x -> x.getMesa().getNumero()).containsExactly(2);
    }

    // ---------- contarPorEstadoJPQL ----------

    @Test
    void contarPorEstado_agrupaYCuenta() {
        Map<EstadoReserva, Long> conteo = new HashMap<>();
        for (Object[] fila : reservaRepository.contarPorEstadoJPQL()) {
            conteo.put((EstadoReserva) fila[0], ((Number) fila[1]).longValue());
        }

        assertThat(conteo)
                .containsEntry(EstadoReserva.CONFIRMADA, 1L)
                .containsEntry(EstadoReserva.PENDIENTE, 1L)
                .containsEntry(EstadoReserva.CANCELADA, 1L);
    }

    // ---------- DetalleReservaRepository ----------

    @Test
    void platosMasReservados_excluyeCanceladas() {
        List<Object[]> top = detalleReservaRepository
                .platosMasReservadosJPQL(EstadoReserva.CANCELADA, PageRequest.of(0, 5));

        // Con la cancelada (Chicha x5) la Chicha ganaría con 6; sin ella: Lomo 3, Chicha 1.
        assertThat(top).hasSize(2);
        assertThat(top.get(0)[0]).isEqualTo("Lomo Saltado");
        assertThat(((Number) top.get(0)[1]).longValue()).isEqualTo(3L);
        assertThat(top.get(1)[0]).isEqualTo("Chicha Morada");
        assertThat(((Number) top.get(1)[1]).longValue()).isEqualTo(1L);
    }

    @Test
    void platosMasReservados_respetaElLimiteDelPageable() {
        List<Object[]> top = detalleReservaRepository
                .platosMasReservadosJPQL(EstadoReserva.CANCELADA, PageRequest.of(0, 1));

        assertThat(top).hasSize(1);
        assertThat(top.get(0)[0]).isEqualTo("Lomo Saltado");
    }

    @Test
    void ingresoEstimado_excluyeLaReservaIndicada() {
        // Sin canceladas: Lomo 3 x 30 + Chicha 1 x 10 = 100.00
        assertThat(detalleReservaRepository.ingresoEstimadoJPQL(EstadoReserva.CANCELADA))
                .isEqualByComparingTo("100.00");

        // Sin pendientes: r1 (2x30 + 1x10 = 70) + r3 (5x10 = 50) = 120.00
        assertThat(detalleReservaRepository.ingresoEstimadoJPQL(EstadoReserva.PENDIENTE))
                .isEqualByComparingTo("120.00");
    }
}