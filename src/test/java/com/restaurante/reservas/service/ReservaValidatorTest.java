package com.restaurante.reservas.service;

import com.restaurante.reservas.model.CategoriaPlato;
import com.restaurante.reservas.model.EstadoMesa;
import com.restaurante.reservas.model.Mesa;
import com.restaurante.reservas.model.Plato;
import com.restaurante.reservas.model.Reserva;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests TDD (RED -> GREEN -> REFACTOR) para la nueva clase ReservaValidator,
 * extraída de ReservaService.crearReserva mediante Sprout Method.
 */
class ReservaValidatorTest {

    private final ReservaValidator validator = new ReservaValidator();

    @Test
    void platoPrincipalNulo_lanzaIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> validator.validarPlatoPrincipalPresente(null));
    }

    @Test
    void mesaNoLibre_lanzaIllegalStateException() {
        Mesa mesa = new Mesa(1, 4);
        mesa.setEstado(EstadoMesa.RESERVADA);

        assertThrows(IllegalStateException.class,
                () -> validator.validarMesaDisponible(mesa));
    }

    @Test
    void reservaActivaPresente_lanzaIllegalStateException() {
        Optional<Reserva> reservaActiva = Optional.of(new Reserva());

        assertThrows(IllegalStateException.class,
                () -> validator.validarSinReservaActiva(reservaActiva));
    }

    @Test
    void categoriaDePlatoIncorrecta_lanzaIllegalArgumentExceptionConMensaje() {
        Plato bebida = new Plato("Chicha morada", "desc", BigDecimal.TEN, CategoriaPlato.BEBIDA);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> validator.validarCategoria(bebida, CategoriaPlato.PRINCIPAL,
                        "El plato seleccionado no es un plato principal."));

        assertEquals("El plato seleccionado no es un plato principal.", ex.getMessage());
    }

    @Test
    void categoriaDePlatoCorrecta_noLanzaExcepcion() {
        Plato principal = new Plato("Lomo saltado", "desc", BigDecimal.TEN, CategoriaPlato.PRINCIPAL);

        assertDoesNotThrow(() ->
                validator.validarCategoria(principal, CategoriaPlato.PRINCIPAL, "mensaje no usado"));
    }
}
