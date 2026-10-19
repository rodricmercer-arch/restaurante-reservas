package com.restaurante.reservas.repository;

import com.restaurante.reservas.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import com.restaurante.reservas.model.Rol;
import java.util.List;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByDni(String dni);

    boolean existsByDni(String dni);

    List<Usuario> findAllByOrderByNombreAsc();

    long countByRol(Rol rol);
}
