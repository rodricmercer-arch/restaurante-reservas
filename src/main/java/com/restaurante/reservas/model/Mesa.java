package com.restaurante.reservas.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "mesas")
public class Mesa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "El número de mesa es obligatorio")
    @Min(value = 1, message = "El número de mesa debe ser mayor o igual a 1")
    @Column(unique = true, nullable = false)
    private Integer numero;

    @NotNull(message = "La capacidad es obligatoria")
    @Min(value = 1, message = "La capacidad mínima es 1 persona")
    @Max(value = 20, message = "La capacidad máxima es 20 personas")
    @Column(nullable = false)
    private Integer capacidad;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoMesa estado = EstadoMesa.LIBRE;

    // Bloqueo optimista: Hibernate suma 1 en cada UPDATE y falla si otro usuario modificó la fila antes.
    @Version
    private Long version;

    public Mesa() {}

    public Mesa(Integer numero, Integer capacidad) {
        this.numero = numero;
        this.capacidad = capacidad;
        this.estado = EstadoMesa.LIBRE;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getNumero() { return numero; }
    public void setNumero(Integer numero) { this.numero = numero; }

    public Integer getCapacidad() { return capacidad; }
    public void setCapacidad(Integer capacidad) { this.capacidad = capacidad; }

    public EstadoMesa getEstado() { return estado; }
    public void setEstado(EstadoMesa estado) { this.estado = estado; }
    public Long getVersion() { return version; }
}
