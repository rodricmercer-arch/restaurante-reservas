package com.restaurante.reservas.dto;

import com.restaurante.reservas.model.CategoriaPlato;
import com.restaurante.reservas.model.DetalleReserva;
import com.restaurante.reservas.model.EstadoReserva;
import com.restaurante.reservas.model.Mesa;
import com.restaurante.reservas.model.Reserva;
import com.restaurante.reservas.model.Usuario;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Salida de una reserva. Nunca expone entidades JPA ni la contraseña del cliente. */
public record ReservaResponseDTO(
        Long id,
        LocalDateTime fechaHora,
        EstadoReserva estado,
        MesaResumen mesa,
        ClienteResumen cliente,
        List<DetalleResumen> detalles) {

    public record MesaResumen(Long id, Integer numero, Integer capacidad) {
        static MesaResumen fromEntity(Mesa m) {
            return new MesaResumen(m.getId(), m.getNumero(), m.getCapacidad());
        }
    }

    public record ClienteResumen(String dni, String nombre) {
        static ClienteResumen fromEntity(Usuario u) {
            return new ClienteResumen(u.getDni(), u.getNombre());
        }
    }

    public record DetalleResumen(Long platoId, String nombre, CategoriaPlato categoria,
                                 Integer cantidad, BigDecimal precio) {
        static DetalleResumen fromEntity(DetalleReserva d) {
            return new DetalleResumen(
                    d.getPlato().getId(),
                    d.getPlato().getNombre(),
                    d.getPlato().getCategoria(),
                    d.getCantidad(),
                    d.getPlato().getPrecio());
        }
    }

    /** Debe llamarse con la sesión de Hibernate abierta (detalles es LAZY); open-in-view lo permite en el controlador. */
    public static ReservaResponseDTO fromEntity(Reserva r) {
        return new ReservaResponseDTO(
                r.getId(),
                r.getFechaHora(),
                r.getEstado(),
                MesaResumen.fromEntity(r.getMesa()),
                ClienteResumen.fromEntity(r.getCliente()),
                r.getDetalles().stream().map(DetalleResumen::fromEntity).toList());
    }
}