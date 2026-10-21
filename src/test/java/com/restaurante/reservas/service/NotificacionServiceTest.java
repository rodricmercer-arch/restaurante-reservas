package com.restaurante.reservas.service;

import com.restaurante.reservas.model.CategoriaPlato;
import com.restaurante.reservas.model.DetalleReserva;
import com.restaurante.reservas.model.Mesa;
import com.restaurante.reservas.model.Plato;
import com.restaurante.reservas.model.Reserva;
import com.restaurante.reservas.model.Rol;
import com.restaurante.reservas.model.Usuario;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class NotificacionServiceTest {

    private Reserva reservaDePrueba() {
        Reserva reserva = new Reserva();
        reserva.setCliente(new Usuario("12345678", "x", "Ana Pérez", Rol.CLIENTE));
        reserva.setMesa(new Mesa(3, 4));
        reserva.setFechaHora(LocalDateTime.of(2026, 10, 20, 19, 30));

        Plato lomo = new Plato("Lomo Saltado", "Salteado de res", new BigDecimal("32.00"), CategoriaPlato.PRINCIPAL);
        Plato chicha = new Plato("Chicha Morada", "Bebida de maíz morado", new BigDecimal("12.00"), CategoriaPlato.BEBIDA);
        reserva.addDetalle(new DetalleReserva(lomo, 1));
        reserva.addDetalle(new DetalleReserva(chicha, 1));
        return reserva;
    }

    @Test
    void construirMensaje_incluyeLosDatosDeLaReservaYNoElDni() {
        String texto = NotificacionService.construirMensaje(reservaDePrueba());

        assertThat(texto)
                .contains("Ana Pérez", "Mesa: 3", "20/10/2026 19:30",
                        "Lomo Saltado", "Chicha Morada", "Total: S/ 44.00")
                .contains("- Lomo Saltado x1 (S/ 32.00)")
                .doesNotContain("12345678");
    }

    @Test
    void notificarReservaNueva_conCanalInactivo_noLanzaExcepcion() {
        NotificacionService servicio = new NotificacionService(false, "", "");

        assertThatCode(() -> servicio.notificarReservaNueva(reservaDePrueba()))
                .doesNotThrowAnyException();
    }
}