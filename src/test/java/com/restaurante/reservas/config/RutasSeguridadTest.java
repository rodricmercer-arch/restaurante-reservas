package com.restaurante.reservas.config;

import com.restaurante.reservas.controller.PerfilController;
import com.restaurante.reservas.controller.ReporteController;
import com.restaurante.reservas.dto.ReporteDTO;
import com.restaurante.reservas.model.Rol;
import com.restaurante.reservas.model.Usuario;
import com.restaurante.reservas.repository.ReservaRepository;
import com.restaurante.reservas.repository.UsuarioRepository;
import com.restaurante.reservas.service.ReporteService;
import com.restaurante.reservas.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifica las reglas de acceso de SecurityConfig para /reportes y /perfil:
 * anónimo -> login, CLIENTE -> 403 en /reportes y 200 en /perfil, ADMIN -> 200 en ambos.
 */
@WebMvcTest({ReporteController.class, PerfilController.class})
@Import(SecurityConfig.class)
class RutasSeguridadTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReporteService reporteService;

    @MockBean
    private UsuarioService usuarioService;

    @MockBean
    private UsuarioRepository usuarioRepository;

    @MockBean
    private ReservaRepository reservaRepository;

    // SecurityConfig lo necesita para construirse dentro del contexto reducido de @WebMvcTest.
    @MockBean
    private AppUserDetailsService appUserDetailsService;

    // apiChain usa oauth2ResourceServer, que exige un JwtDecoder para construirse.
    // JwtConfig no entra en @WebMvcTest, así que se simula aquí.
    @MockBean
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void datosDeApoyo() {
        // Las vistas se renderizan de verdad, así que los mocks deben devolver datos (no null).
        Usuario cliente = new Usuario("12345678", "x", "Cliente Test", Rol.CLIENTE);
        cliente.setId(1L);
        Usuario admin = new Usuario("00000000", "x", "Administrador", Rol.ADMIN);
        admin.setId(2L);
        when(usuarioRepository.findByDni("12345678")).thenReturn(Optional.of(cliente));
        when(usuarioRepository.findByDni("00000000")).thenReturn(Optional.of(admin));

        when(reporteService.generar()).thenReturn(
                new ReporteDTO(Map.of(), Map.of(), Map.of(), List.of(), BigDecimal.ZERO));
    }

    // ---------- Anónimo: redirige al login ("/login") ----------

    @Test
    void anonimo_reportes_redirigeAlLogin() throws Exception {
        mockMvc.perform(get("/reportes"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("http://localhost/login"));
    }

    @Test
    void anonimo_perfil_redirigeAlLogin() throws Exception {
        mockMvc.perform(get("/perfil"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("http://localhost/login"));
    }

    // ---------- CLIENTE ----------

    @Test
    @WithMockUser(username = "12345678", roles = "CLIENTE")
    void cliente_reportes_esProhibido() throws Exception {
        mockMvc.perform(get("/reportes"))
                .andExpect(status().isForbidden())
                .andExpect(forwardedUrl("/403")); // accessDeniedPage("/403")
    }

    @Test
    @WithMockUser(username = "12345678", roles = "CLIENTE")
    void cliente_perfil_esPermitido() throws Exception {
        mockMvc.perform(get("/perfil"))
                .andExpect(status().isOk());
    }

    // ---------- ADMIN ----------

    @Test
    @WithMockUser(username = "00000000", roles = "ADMIN")
    void admin_reportes_esPermitido() throws Exception {
        mockMvc.perform(get("/reportes"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "00000000", roles = "ADMIN")
    void admin_perfil_esPermitido() throws Exception {
        mockMvc.perform(get("/perfil"))
                .andExpect(status().isOk());
    }
}