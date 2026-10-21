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
import static org.mockito.Mockito.*;

/**
 * Pruebas de ReservaService.cancelarReserva y moverReserva (Fase 2).
 * Mismo estilo que la prueba de caracterización: Mockito puro, sin Spring ni base de datos.
 */
@ExtendWith(MockitoExtension.class)
class ReservaServiceCancelarMoverTest {

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private MesaRepository mesaRepository;

    @Mock
    private PlatoRepository platoRepository;

    private ReservaService reservaService;

    private Usuario cliente;
    private Mesa mesaActual;
    private Reserva reserva;

    @BeforeEach
    void setUp() {
        reservaService = new ReservaService(reservaRepository, mesaRepository, platoRepository);

        cliente = new Usuario("12345678", "pass", "Cliente Test", Rol.CLIENTE);
        cliente.setId(1L);

        mesaActual = new Mesa(5, 4);
        mesaActual.setId(10L);
        mesaActual.setEstado(EstadoMesa.RESERVADA);

        reserva = new Reserva();
        reserva.setId(100L);
        reserva.setCliente(cliente);
        reserva.setMesa(mesaActual);
        reserva.setEstado(EstadoReserva.CONFIRMADA);
    }

    @Test
    void cancelarDosVeces_laSegundaFalla_yNoVuelveALiberarLaMesa() {
        when(reservaRepository.findById(100L)).thenReturn(Optional.of(reserva));

        reservaService.cancelarReserva(100L, cliente); // primera vez: correcta

        assertThat(reserva.getEstado()).isEqualTo(EstadoReserva.CANCELADA);
        assertThat(mesaActual.getEstado()).isEqualTo(EstadoMesa.LIBRE);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> reservaService.cancelarReserva(100L, cliente));

        assertThat(ex.getMessage()).isEqualTo("La reserva ya está cancelada.");
        // Solo se guardó una vez (la de la primera cancelación).
        verify(mesaRepository, times(1)).save(mesaActual);
        verify(reservaRepository, times(1)).save(reserva);
    }

    @Test
    void moverReservaCancelada_lanzaIllegalStateException() {
        reserva.setEstado(EstadoReserva.CANCELADA);
        when(reservaRepository.findById(100L)).thenReturn(Optional.of(reserva));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> reservaService.moverReserva(100L, 20L));

        assertThat(ex.getMessage()).isEqualTo("No se puede mover una reserva cancelada.");
        verifyNoInteractions(mesaRepository);
        verify(reservaRepository, never()).save(any(Reserva.class));
    }

    @Test
    void moverALaMismaMesa_lanzaIllegalStateException() {
        when(reservaRepository.findById(100L)).thenReturn(Optional.of(reserva));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> reservaService.moverReserva(100L, 10L)); // 10L = mesa actual

        assertThat(ex.getMessage()).isEqualTo("La reserva ya está en esa mesa.");
        verifyNoInteractions(mesaRepository);
        verify(reservaRepository, never()).save(any(Reserva.class));
        assertThat(mesaActual.getEstado()).isEqualTo(EstadoMesa.RESERVADA);
    }

    @Test
    void moverReservaFeliz_liberaMesaAnteriorYReservaLaNueva() {
        Mesa nuevaMesa = new Mesa(6, 4);
        nuevaMesa.setId(20L);
        nuevaMesa.setEstado(EstadoMesa.LIBRE);

        when(reservaRepository.findById(100L)).thenReturn(Optional.of(reserva));
        when(mesaRepository.findById(20L)).thenReturn(Optional.of(nuevaMesa));

        reservaService.moverReserva(100L, 20L);

        assertThat(mesaActual.getEstado()).isEqualTo(EstadoMesa.LIBRE);
        assertThat(nuevaMesa.getEstado()).isEqualTo(EstadoMesa.RESERVADA);
        assertThat(reserva.getMesa()).isSameAs(nuevaMesa);
        assertThat(reserva.getEstado()).isEqualTo(EstadoReserva.CONFIRMADA);
        verify(mesaRepository).save(mesaActual);
        verify(mesaRepository).save(nuevaMesa);
        verify(reservaRepository).save(reserva);
    }
}