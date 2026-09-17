package com.restaurante.reservas.service;

import com.restaurante.reservas.model.CategoriaPlato;
import com.restaurante.reservas.model.EstadoMesa;
import com.restaurante.reservas.model.Mesa;
import com.restaurante.reservas.model.Plato;
import com.restaurante.reservas.model.Reserva;

import java.util.Optional;

/**
 * Validaciones de negocio para la creación de reservas.
 * Extraído de ReservaService.crearReserva mediante Sprout Method,
 * desarrollado con TDD (RED -> GREEN -> REFACTOR).
 */
public class ReservaValidator {

    public void validarPlatoPrincipalPresente(Long platoPrincipalId) {
        if (platoPrincipalId == null) {
            throw new IllegalArgumentException("Debes elegir un plato principal.");
        }
    }

    public void validarMesaDisponible(Mesa mesa) {
        if (mesa.getEstado() != EstadoMesa.LIBRE) {
            throw new IllegalStateException("Esa mesa ya está reservada.");
        }
    }

    public void validarSinReservaActiva(Optional<Reserva> reservaActiva) {
        if (reservaActiva.isPresent()) {
            throw new IllegalStateException("Ya tienes una reserva activa. Cancélala antes de reservar otra mesa.");
        }
    }

    public void validarCategoria(Plato plato, CategoriaPlato categoriaEsperada, String mensajeError) {
        if (plato.getCategoria() != categoriaEsperada) {
            throw new IllegalArgumentException(mensajeError);
        }
    }
}
