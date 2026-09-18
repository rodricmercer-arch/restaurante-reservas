package com.restaurante.reservas.service;

import com.restaurante.reservas.model.*;
import com.restaurante.reservas.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ReservaService {

    private final ReservaRepository reservaRepository;
    private final MesaRepository mesaRepository;
    private final PlatoRepository platoRepository;
    private final ReservaValidator validator = new ReservaValidator();

    public ReservaService(ReservaRepository reservaRepository,
                           MesaRepository mesaRepository,
                           PlatoRepository platoRepository) {
        this.reservaRepository = reservaRepository;
        this.mesaRepository = mesaRepository;
        this.platoRepository = platoRepository;
    }

    private static final List<EstadoReserva> ESTADOS_ACTIVOS = List.of(EstadoReserva.PENDIENTE, EstadoReserva.CONFIRMADA);

    public Optional<Reserva> buscarReservaActiva(Usuario cliente) {
        return reservaRepository.findFirstByClienteAndEstadoIn(cliente, ESTADOS_ACTIVOS);
    }

    @Transactional
    public Reserva crearReserva(Usuario cliente, Long mesaId, Long platoPrincipalId,
                                 Long platoPostreId, Long platoBebidaId) {

        // Regla: una mesa/reserva activa por cliente
        validator.validarSinReservaActiva(buscarReservaActiva(cliente));

        Mesa mesa = mesaRepository.findById(mesaId)
                .orElseThrow(() -> new IllegalArgumentException("La mesa no existe."));
        validator.validarMesaDisponible(mesa);

        // Regla: el plato principal es obligatorio
        validator.validarPlatoPrincipalPresente(platoPrincipalId);
        Plato principal = platoRepository.findById(platoPrincipalId)
                .orElseThrow(() -> new IllegalArgumentException("Plato principal inválido."));
        validator.validarCategoria(principal, CategoriaPlato.PRINCIPAL,
                "El plato seleccionado no es un plato principal.");

        Reserva reserva = new Reserva();
        reserva.setCliente(cliente);
        reserva.setMesa(mesa);
        reserva.setEstado(EstadoReserva.CONFIRMADA);
        reserva.addDetalle(new DetalleReserva(principal, 1));

        // Postre: opcional
        if (platoPostreId != null) {
            Plato postre = platoRepository.findById(platoPostreId)
                    .orElseThrow(() -> new IllegalArgumentException("Postre inválido."));
            validator.validarCategoria(postre, CategoriaPlato.POSTRE,
                    "El plato seleccionado no es un postre.");
            reserva.addDetalle(new DetalleReserva(postre, 1));
        }

        // Bebida: opcional
        if (platoBebidaId != null) {
            Plato bebida = platoRepository.findById(platoBebidaId)
                    .orElseThrow(() -> new IllegalArgumentException("Bebida inválida."));
            validator.validarCategoria(bebida, CategoriaPlato.BEBIDA,
                    "El plato seleccionado no es una bebida.");
            reserva.addDetalle(new DetalleReserva(bebida, 1));
        }

        mesa.setEstado(EstadoMesa.RESERVADA);
        mesaRepository.save(mesa);

        return reservaRepository.save(reserva);
    }

    @Transactional
    public void cancelarReserva(Long reservaId, Usuario usuarioActual) {
        Reserva reserva = reservaRepository.findById(reservaId)
                .orElseThrow(() -> new IllegalArgumentException("La reserva no existe."));

        boolean esDueno = reserva.getCliente().getId().equals(usuarioActual.getId());
        boolean esAdmin = usuarioActual.getRol() == Rol.ADMIN;
        if (!esDueno && !esAdmin) {
            throw new IllegalStateException("No tienes permiso para cancelar esta reserva.");
        }

        reserva.setEstado(EstadoReserva.CANCELADA);
        Mesa mesa = reserva.getMesa();
        mesa.setEstado(EstadoMesa.LIBRE);
        mesaRepository.save(mesa);
        reservaRepository.save(reserva);
    }

    @Transactional
    public void moverReserva(Long reservaId, Long nuevaMesaId) {
        Reserva reserva = reservaRepository.findById(reservaId)
                .orElseThrow(() -> new IllegalArgumentException("La reserva no existe."));

        Mesa nuevaMesa = mesaRepository.findById(nuevaMesaId)
                .orElseThrow(() -> new IllegalArgumentException("La mesa destino no existe."));
        if (nuevaMesa.getEstado() != EstadoMesa.LIBRE) {
            throw new IllegalStateException("La mesa destino no está libre.");
        }

        Mesa mesaAnterior = reserva.getMesa();
        mesaAnterior.setEstado(EstadoMesa.LIBRE);
        mesaRepository.save(mesaAnterior);

        nuevaMesa.setEstado(EstadoMesa.RESERVADA);
        mesaRepository.save(nuevaMesa);

        reserva.setMesa(nuevaMesa);
        reservaRepository.save(reserva);
    }

    public List<Reserva> listarTodas() {
        return reservaRepository.findAllByOrderByFechaHoraDesc();
    }
}
