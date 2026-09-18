package com.restaurante.reservas.service;

import com.restaurante.reservas.model.*;
import com.restaurante.reservas.repository.MesaRepository;
import com.restaurante.reservas.repository.PlatoRepository;
import com.restaurante.reservas.repository.ReservaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * Characterization tests de ReservaService.crearReserva.
 *
 * IMPORTANTE: estos tests NO son TDD. Documentan y protegen el comportamiento
 * que el método YA TIENE hoy, antes de cualquier refactorización, para poder
 * refactorizar con seguridad más adelante.
 */
@ExtendWith(MockitoExtension.class)
class ReservaServiceCrearReservaCharacterizationTest {

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private MesaRepository mesaRepository;

    @Mock
    private PlatoRepository platoRepository;

    private ReservaService reservaService;

    private Usuario cliente;
    private Mesa mesa;
    private Plato platoPrincipal;

    @BeforeEach
    void setUp() {
        reservaService = new ReservaService(reservaRepository, mesaRepository, platoRepository);

        cliente = new Usuario("12345678", "pass", "Cliente Test", Rol.CLIENTE);
        cliente.setId(1L);

        mesa = new Mesa(5, 4);
        mesa.setId(10L);
        mesa.setEstado(EstadoMesa.LIBRE);

        platoPrincipal = new Plato("Lomo saltado", "desc", new java.math.BigDecimal("35.00"), CategoriaPlato.PRINCIPAL);
        platoPrincipal.setId(100L);
    }

    @Test
    void clienteSinReservaActivaYMesaLibreYPlatoValido_creaReservaConfirmadaYReservaMesa() {
        when(reservaRepository.findFirstByClienteAndEstadoIn(eq(cliente), anyList()))
                .thenReturn(Optional.empty());
        when(mesaRepository.findById(10L)).thenReturn(Optional.of(mesa));
        when(platoRepository.findById(100L)).thenReturn(Optional.of(platoPrincipal));
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mesaRepository.save(any(Mesa.class))).thenAnswer(inv -> inv.getArgument(0));

        Reserva resultado = reservaService.crearReserva(cliente, 10L, 100L, null, null);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getEstado()).isEqualTo(EstadoReserva.CONFIRMADA);
        assertThat(resultado.getMesa()).isEqualTo(mesa);
        assertThat(resultado.getCliente()).isEqualTo(cliente);
        assertThat(mesa.getEstado()).isEqualTo(EstadoMesa.RESERVADA);
        verify(mesaRepository).save(mesa);
        verify(reservaRepository).save(any(Reserva.class));
    }

    @Test
    void clienteConReservaActiva_lanzaIllegalStateException() {
        Reserva reservaActiva = new Reserva();
        when(reservaRepository.findFirstByClienteAndEstadoIn(eq(cliente), anyList()))
                .thenReturn(Optional.of(reservaActiva));

        assertThrows(IllegalStateException.class,
                () -> reservaService.crearReserva(cliente, 10L, 100L, null, null));

        verifyNoInteractions(mesaRepository);
        verifyNoInteractions(platoRepository);
    }

    @Test
    void mesaQueNoEstaLibre_lanzaIllegalStateException() {
        mesa.setEstado(EstadoMesa.RESERVADA);
        when(reservaRepository.findFirstByClienteAndEstadoIn(eq(cliente), anyList()))
                .thenReturn(Optional.empty());
        when(mesaRepository.findById(10L)).thenReturn(Optional.of(mesa));

        assertThrows(IllegalStateException.class,
                () -> reservaService.crearReserva(cliente, 10L, 100L, null, null));

        verifyNoInteractions(platoRepository);
        verify(mesaRepository, never()).save(any());
        verify(reservaRepository, never()).save(any());
    }

    @Test
    void platoPrincipalNulo_lanzaIllegalArgumentException() {
        when(reservaRepository.findFirstByClienteAndEstadoIn(eq(cliente), anyList()))
                .thenReturn(Optional.empty());
        when(mesaRepository.findById(10L)).thenReturn(Optional.of(mesa));

        assertThrows(IllegalArgumentException.class,
                () -> reservaService.crearReserva(cliente, 10L, null, null, null));

        verifyNoInteractions(platoRepository);
        verify(mesaRepository, never()).save(any());
        verify(reservaRepository, never()).save(any());
    }
}
