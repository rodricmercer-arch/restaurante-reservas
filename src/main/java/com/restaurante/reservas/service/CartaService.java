package com.restaurante.reservas.service;

import com.restaurante.reservas.model.CategoriaPlato;
import com.restaurante.reservas.model.Plato;
import com.restaurante.reservas.repository.PlatoRepository;
import com.restaurante.reservas.repository.DetalleReservaRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

import java.util.List;
import java.util.Optional;

@Service
// Por defecto todo es de solo lectura (transacción más liviana y sin flush accidental); los métodos que escriben llevan su propio @Transactional.
@Transactional(readOnly = true)
public class CartaService {

    private final PlatoRepository platoRepository;
    private final DetalleReservaRepository detalleReservaRepository;

    public CartaService(PlatoRepository platoRepository,
                        DetalleReservaRepository detalleReservaRepository) {
        this.platoRepository = platoRepository;
        this.detalleReservaRepository = detalleReservaRepository;
    }

    public List<Plato> principales() { return platoRepository.findByCategoria(CategoriaPlato.PRINCIPAL); }
    public List<Plato> postres() { return platoRepository.findByCategoria(CategoriaPlato.POSTRE); }
    public List<Plato> bebidas() { return platoRepository.findByCategoria(CategoriaPlato.BEBIDA); }
    public List<Plato> todos() { return platoRepository.findAll(); }
    // Escribe en BD (lo usa la API REST): necesita su propia transacción de escritura porque la clase es readOnly.
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public Plato guardar(Plato p) { return platoRepository.save(p); }
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void eliminar(Long id) {
        if (detalleReservaRepository.existsByPlatoId(id)) {
            throw new IllegalStateException("No se puede eliminar el plato: ya fue usado en reservas.");
        }
        platoRepository.deleteById(id);
    }

    // ---- Métodos añadidos para la API REST (2.3) — no alteran los anteriores ----

    /** Usado por la API REST para GET /api/platos/{id} y para validar antes de PUT/DELETE. */
    public Optional<Plato> buscarPorId(Long id) {
        return platoRepository.findById(id);
    }

    /** Usado por la API REST para GET /api/platos?categoria=... */
    public List<Plato> buscarPorCategoria(CategoriaPlato categoria) {
        return platoRepository.findByCategoria(categoria);
    }
    // ---- Fase 1: CRUD con edición desde el panel admin ----

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public Plato crear(Plato datos) {
        if (platoRepository.existsByNombreIgnoreCase(datos.getNombre())) {
            throw new IllegalStateException("Ya existe un plato con el nombre \"" + datos.getNombre() + "\".");
        }
        Plato nuevo = new Plato(datos.getNombre(), datos.getDescripcion(),
                datos.getPrecio(), datos.getCategoria());
        nuevo.setImagen(normalizarImagen(datos.getImagen()));
        return platoRepository.save(nuevo);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public Plato actualizar(Long id, Plato datos) {
        Plato plato = platoRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("El plato que intenta editar no existe."));
        if (platoRepository.existsByNombreIgnoreCaseAndIdNot(datos.getNombre(), id)) {
            throw new IllegalStateException("Ya existe un plato con el nombre \"" + datos.getNombre() + "\".");
        }
        plato.setNombre(datos.getNombre());
        plato.setDescripcion(datos.getDescripcion());
        plato.setPrecio(datos.getPrecio());
        plato.setCategoria(datos.getCategoria());
        plato.setImagen(normalizarImagen(datos.getImagen()));
        return platoRepository.save(plato);
    }

    /** Imagen nula o en blanco -> null; en otro caso se guarda recortada con trim(). */
    private String normalizarImagen(String imagen) {
        if (imagen == null || imagen.isBlank()) {
            return null;
        }
        return imagen.trim();
    }

    // ---- Fase 2: consultas JPQL ----

    /**
     * Búsqueda avanzada de platos para el panel admin.
     * NUNCA se pasa null a la consulta JPQL: según el motor, un parámetro null no tiene tipo definido
     * y falla en funciones como LOWER() o en IN :lista. Por eso cada filtro vacío se cambia por un valor
     * "neutro" que no filtra nada: texto -> "", categoría -> todas las categorías,
     * precioMin -> 0 y precioMax -> 99999999.99.
     */
    public List<Plato> buscarAvanzado(String texto, CategoriaPlato categoria,
                                      BigDecimal precioMin, BigDecimal precioMax) {
        String textoSeguro = (texto == null) ? "" : texto.trim();
        List<CategoriaPlato> categorias = (categoria == null)
                ? List.of(CategoriaPlato.values())
                : List.of(categoria);
        BigDecimal min = (precioMin == null) ? BigDecimal.ZERO : precioMin;
        BigDecimal max = (precioMax == null) ? new BigDecimal("99999999.99") : precioMax;
        return platoRepository.buscarAvanzadoJPQL(textoSeguro, categorias, min, max);
    }
}
