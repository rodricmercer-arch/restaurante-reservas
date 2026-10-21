package com.restaurante.reservas.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "detalle_reserva")
public class DetalleReserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "reserva_id")
    private Reserva reserva;

    @ManyToOne(optional = false)
    @JoinColumn(name = "plato_id")
    private Plato plato;

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad debe ser al menos 1")
    @Column(nullable = false)
    private Integer cantidad = 1;

    public DetalleReserva() {}

    public DetalleReserva(Plato plato, Integer cantidad) {
        this.plato = plato;
        this.cantidad = cantidad;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Reserva getReserva() { return reserva; }
    public void setReserva(Reserva reserva) { this.reserva = reserva; }

    public Plato getPlato() { return plato; }
    public void setPlato(Plato plato) { this.plato = plato; }

    public Integer getCantidad() { return cantidad; }
    public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }
}
