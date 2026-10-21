package com.restaurante.reservas.service;

import com.restaurante.reservas.model.Mesa;
import com.restaurante.reservas.repository.MesaRepository;
import com.restaurante.reservas.repository.ReservaRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
// Por defecto todo es de solo lectura (transacción más liviana y sin flush accidental); los métodos que escriben llevan su propio @Transactional.
@Transactional(readOnly = true)
public class MesaService {

    private final MesaRepository mesaRepository;
    private final ReservaRepository reservaRepository;

    public MesaService(MesaRepository mesaRepository, ReservaRepository reservaRepository) {
        this.mesaRepository = mesaRepository;
        this.reservaRepository = reservaRepository;
    }

    public List<Mesa> listarTodas() { return mesaRepository.findAllByOrderByNumeroAsc(); }
    // Escribe en BD: necesita su propia transacción de escritura porque la clase es readOnly.
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public Mesa guardar(Mesa m) { return mesaRepository.save(m); }

    public Optional<Mesa> buscarPorId(Long id) {
        return mesaRepository.findById(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public Mesa crear(Mesa datos) {
        if (mesaRepository.existsByNumero(datos.getNumero())) {
            throw new IllegalStateException("Ya existe una mesa con el número " + datos.getNumero() + ".");
        }
        // Siempre se construye una mesa nueva: el estado nace LIBRE aunque el formulario envíe otro.
        return mesaRepository.save(new Mesa(datos.getNumero(), datos.getCapacidad()));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public Mesa actualizar(Long id, Mesa datos) {
        Mesa mesa = mesaRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("La mesa que intenta editar no existe."));
        if (mesaRepository.existsByNumeroAndIdNot(datos.getNumero(), id)) {
            throw new IllegalStateException("Ya existe una mesa con el número " + datos.getNumero() + ".");
        }
        // Solo número y capacidad; el estado nunca se toca desde el formulario.
        mesa.setNumero(datos.getNumero());
        mesa.setCapacidad(datos.getCapacidad());
        return mesaRepository.save(mesa);
    }
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void eliminar(Long id) {
        if (reservaRepository.existsByMesaId(id)) {
            throw new IllegalStateException("No se puede eliminar la mesa: tiene reservas registradas.");
        }
        mesaRepository.deleteById(id);
    }
}
