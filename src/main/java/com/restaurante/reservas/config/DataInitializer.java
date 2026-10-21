package com.restaurante.reservas.config;

import com.restaurante.reservas.model.*;
import com.restaurante.reservas.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

@Component
public class DataInitializer implements CommandLineRunner {

    // Foto por defecto de cada plato de ejemplo: clave = nombre exacto del plato; valor = ruta dentro de static/ (sin barra inicial).
    private static final Map<String, String> IMAGENES_PLATOS = Map.of(
            "Lomo Saltado", "img/platos/lomo-saltado.jpg",
            "Arroz con Pollo", "img/platos/arroz-con-pollo.jpg",
            "Tallarines Verdes", "img/platos/tallarines-verdes.jpg",
            "Suspiro a la Limeña", "img/platos/suspiro-a-la-limena.jpg",
            "Mazamorra Morada", "img/platos/mazamorra-morada.jpg",
            "Chicha Morada", "img/platos/chicha-morada.jpg",
            "Gaseosa", "img/platos/gaseosa.jpg"
    );

    private final UsuarioRepository usuarioRepository;
    private final MesaRepository mesaRepository;
    private final PlatoRepository platoRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UsuarioRepository usuarioRepository, MesaRepository mesaRepository,
                            PlatoRepository platoRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.mesaRepository = mesaRepository;
        this.platoRepository = platoRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {

        // Usuario admin por defecto
        if (usuarioRepository.findByDni("00000000").isEmpty()) {
            usuarioRepository.save(new Usuario("00000000", passwordEncoder.encode("admin123"), "Administrador", Rol.ADMIN));
        }

        // Usuario cliente de prueba
        if (usuarioRepository.findByDni("11111111").isEmpty()) {
            usuarioRepository.save(new Usuario("11111111", passwordEncoder.encode("cliente123"), "Cliente Demo", Rol.CLIENTE));
        }

        // Mesas de ejemplo
        if (mesaRepository.count() == 0) {
            for (int i = 1; i <= 8; i++) {
                mesaRepository.save(new Mesa(i, (i % 2 == 0) ? 4 : 2));
            }
        }

        // Carta de ejemplo
        if (platoRepository.count() == 0) {
            platoRepository.save(new Plato("Lomo Saltado", "Clásico peruano con papas fritas", new BigDecimal("32.00"), CategoriaPlato.PRINCIPAL));
            platoRepository.save(new Plato("Arroz con Pollo", "Arroz verde con pollo y papa a la huancaína", new BigDecimal("28.00"), CategoriaPlato.PRINCIPAL));
            platoRepository.save(new Plato("Tallarines Verdes", "Con bistec apanado", new BigDecimal("30.00"), CategoriaPlato.PRINCIPAL));

            platoRepository.save(new Plato("Suspiro a la Limeña", "Postre tradicional", new BigDecimal("12.00"), CategoriaPlato.POSTRE));
            platoRepository.save(new Plato("Mazamorra Morada", "Postre a base de maíz morado", new BigDecimal("10.00"), CategoriaPlato.POSTRE));

            platoRepository.save(new Plato("Chicha Morada", "Bebida tradicional", new BigDecimal("8.00"), CategoriaPlato.BEBIDA));
            platoRepository.save(new Plato("Gaseosa", "500 ml", new BigDecimal("6.00"), CategoriaPlato.BEBIDA));
        }

        // Imágenes por defecto: rellena los platos (nuevos o ya existentes) que todavía no tengan imagen.
        for (Plato plato : platoRepository.findAll()) {
            boolean sinImagen = plato.getImagen() == null || plato.getImagen().isBlank();
            String ruta = IMAGENES_PLATOS.get(plato.getNombre());
            if (sinImagen && ruta != null) {
                plato.setImagen(ruta);
                platoRepository.save(plato);
            }
        }
    }
}