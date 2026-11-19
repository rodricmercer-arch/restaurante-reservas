package com.restaurante.reservas.dto;

import com.restaurante.reservas.model.CategoriaPlato;
import com.restaurante.reservas.model.Plato;

import java.math.BigDecimal;

/**
 * DTO de salida devuelto por la API REST para representar un Plato.
 * Incluye el id (a diferencia de PlatoRequestDTO) porque representa
 * un recurso ya persistido.
 */
public class PlatoResponseDTO {

    private Long id;
    private String nombre;
    private String descripcion;
    private BigDecimal precio;
    private CategoriaPlato categoria;
    private String imagen;

    public PlatoResponseDTO() {
    }

    public PlatoResponseDTO(Long id, String nombre, String descripcion, BigDecimal precio, CategoriaPlato categoria) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.precio = precio;
        this.categoria = categoria;
    }

    public PlatoResponseDTO(Long id, String nombre, String descripcion, BigDecimal precio,
                            CategoriaPlato categoria, String imagen) {
        this(id, nombre, descripcion, precio, categoria);
        this.imagen = imagen;
    }

    /**
     * Fábrica de conveniencia para mapear la entidad JPA Plato a este DTO,
     * sin exponer la entidad directamente en las respuestas de la API.
     */
    public static PlatoResponseDTO fromEntity(Plato plato) {
        return new PlatoResponseDTO(
                plato.getId(),
                plato.getNombre(),
                plato.getDescripcion(),
                plato.getPrecio(),
                plato.getCategoria(),
                plato.getImagen()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public void setPrecio(BigDecimal precio) {
        this.precio = precio;
    }

    public CategoriaPlato getCategoria() {
        return categoria;
    }

    public void setCategoria(CategoriaPlato categoria) {
        this.categoria = categoria;
    }

    public String getImagen() {
        return imagen;
    }

    public void setImagen(String imagen) {
        this.imagen = imagen;
    }
}
