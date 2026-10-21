package com.restaurante.reservas.service;

import com.restaurante.reservas.model.*;
import com.restaurante.reservas.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import java.util.List;
import java.util.Optional;

@Service
// Por defecto todo es de solo lectura (transacción más liviana y sin flush accidental); los métodos que escriben llevan su propio @Transactional.
@Transactional(readOnly = true)
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

    // Escritura con rollback ante cualquier excepción (incluso comprobadas): mesa y reserva se guardan juntas o ninguna.
    @Transactional(rollbackFor = Exception.class)
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

    // Escritura con rollback ante cualquier excepción: reserva y mesa se actualizan juntas o ninguna.
    @Transactional(rollbackFor = Exception.class)
    public void cancelarReserva(Long reservaId, Usuario usuarioActual) {
        Reserva reserva = reservaRepository.findById(reservaId)
                .orElseThrow(() -> new IllegalArgumentException("La reserva no existe."));

        boolean esDueno = reserva.getCliente().getId().equals(usuarioActual.getId());
        boolean esAdmin = usuarioActual.getRol() == Rol.ADMIN;
        if (!esDueno && !esAdmin) {
            throw new IllegalStateException("No tienes permiso para cancelar esta reserva.");
        }

        // Cancelar dos veces liberaría la mesa aunque otro cliente ya la haya reservado.
        if (reserva.getEstado() == EstadoReserva.CANCELADA) {
            throw new IllegalStateException("La reserva ya está cancelada.");
        }

        reserva.setEstado(EstadoReserva.CANCELADA);
        Mesa mesa = reserva.getMesa();
        mesa.setEstado(EstadoMesa.LIBRE);
        mesaRepository.save(mesa);
        reservaRepository.save(reserva);
    }

    // Escritura con rollback ante cualquier excepción: las dos mesas y la reserva cambian juntas o ninguna.
    @Transactional(rollbackFor = Exception.class)
    public void moverReserva(Long reservaId, Long nuevaMesaId) {
        Reserva reserva = reservaRepository.findById(reservaId)
                .orElseThrow(() -> new IllegalArgumentException("La reserva no existe."));

        // Una reserva cancelada ya liberó su mesa; moverla ocuparía una mesa sin reserva válida.
        if (reserva.getEstado() == EstadoReserva.CANCELADA) {
            throw new IllegalStateException("No se puede mover una reserva cancelada.");
        }
        // Va antes de buscar la mesa destino: la mesa actual está RESERVADA y daría un error engañoso ("no está libre").
        if (reserva.getMesa().getId().equals(nuevaMesaId)) {
            throw new IllegalStateException("La reserva ya está en esa mesa.");
        }

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

    /**
     * Filtra reservas para el panel admin. NUNCA se pasa null a la consulta JPQL (un parámetro null
     * no tiene tipo definido y puede fallar en LIKE/IN/BETWEEN), así que cada filtro vacío se sustituye:
     * dni -> "", estado -> todos los estados, desde -> 2000-01-01 00:00 y
     * hasta -> 2999-12-31 23:59:59.999999999.
     * "hasta" llega al último instante de ese día, para incluir todas las reservas de esa fecha.
     */
    public List<Reserva> buscarConFiltros(String dni, EstadoReserva estado,
                                          LocalDate desde, LocalDate hasta) {
        String dniSeguro = (dni == null) ? "" : dni.trim();
        List<EstadoReserva> estados = (estado == null)
                ? List.of(EstadoReserva.values())
                : List.of(estado);
        LocalDateTime desdeSeguro = (desde == null)
                ? LocalDateTime.of(2000, 1, 1, 0, 0)
                : desde.atStartOfDay();
        LocalDateTime hastaSeguro = (hasta == null)
                ? LocalDateTime.of(2999, 12, 31, 23, 59, 59, 999_999_999)
                : hasta.atTime(LocalTime.MAX);
        return reservaRepository.buscarConFiltrosJPQL(dniSeguro, estados, desdeSeguro, hastaSeguro);
    }
}
