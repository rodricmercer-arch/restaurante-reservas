package com.restaurante.reservas.service;

import com.restaurante.reservas.model.CategoriaPlato;
import com.restaurante.reservas.model.Plato;
import com.restaurante.reservas.repository.PlatoRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CartaService {

    private final PlatoRepository platoRepository;

    public CartaService(PlatoRepository platoRepository) {
        this.platoRepository = platoRepository;
    }

    public List<Plato> principales() { return platoRepository.findByCategoria(CategoriaPlato.PRINCIPAL); }
    public List<Plato> postres() { return platoRepository.findByCategoria(CategoriaPlato.POSTRE); }
    public List<Plato> bebidas() { return platoRepository.findByCategoria(CategoriaPlato.BEBIDA); }
    public List<Plato> todos() { return platoRepository.findAll(); }
    public Plato guardar(Plato p) { return platoRepository.save(p); }
    public void eliminar(Long id) { platoRepository.deleteById(id); }

    // ---- Métodos añadidos para la API REST (2.3) — no alteran los anteriores ----

    /** Usado por la API REST para GET /api/platos/{id} y para validar antes de PUT/DELETE. */
    public Optional<Plato> buscarPorId(Long id) {
        return platoRepository.findById(id);
    }

    /** Usado por la API REST para GET /api/platos?categoria=... */
    public List<Plato> buscarPorCategoria(CategoriaPlato categoria) {
        return platoRepository.findByCategoria(categoria);
    }
}
