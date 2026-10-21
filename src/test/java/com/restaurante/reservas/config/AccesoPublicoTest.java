package com.restaurante.reservas.config;

import com.restaurante.reservas.controller.HomeController;
import com.restaurante.reservas.controller.RegistroController;
import com.restaurante.reservas.repository.UsuarioRepository;
import com.restaurante.reservas.service.CartaService;
import com.restaurante.reservas.service.MesaService;
import com.restaurante.reservas.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Verifica el acceso público (/, /carta, /login, /registro), el registro de clientes
 * y que las rutas privadas siguen protegidas (redirigen a /login).
 */
@WebMvcTest({HomeController.class, RegistroController.class})
@Import(SecurityConfig.class)
class AccesoPublicoTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UsuarioRepository usuarioRepository;

    @MockBean
    private MesaService mesaService;

    @MockBean
    private CartaService cartaService;

    @MockBean
    private UsuarioService usuarioService;

    // SecurityConfig lo necesita para construirse dentro del contexto reducido de @WebMvcTest.
    @MockBean
    private AppUserDetailsService appUserDetailsService;

    // apiChain usa oauth2ResourceServer, que exige un JwtDecoder para construirse.
    @MockBean
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void datosDeApoyo() {
        // Las vistas se renderizan de verdad: las listas deben ser vacías, nunca null.
        when(mesaService.listarTodas()).thenReturn(List.of());
        when(cartaService.principales()).thenReturn(List.of());
        when(cartaService.postres()).thenReturn(List.of());
        when(cartaService.bebidas()).thenReturn(List.of());
    }

    // ---------- Páginas públicas ----------

    @Test
    void anonimo_inicio_muestraMesasPublicas() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("cliente/mesas"));
    }

    @Test
    void anonimo_carta_esPublica() throws Exception {
        mockMvc.perform(get("/carta"))
                .andExpect(status().isOk())
                .andExpect(view().name("publico/carta"));
    }

    @Test
    void login_esPublico() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk());
    }

    @Test
    void registro_formularioEsPublico() throws Exception {
        mockMvc.perform(get("/registro"))
                .andExpect(status().isOk())
                .andExpect(view().name("registro"));
    }

    // ---------- Rutas privadas siguen protegidas ----------

    @Test
    void anonimo_clienteMesas_redirigeAlLogin() throws Exception {
        mockMvc.perform(get("/cliente/mesas"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("http://localhost/login"));
    }

    @Test
    void anonimo_clienteReservar_redirigeAlLogin() throws Exception {
        mockMvc.perform(get("/cliente/reservar/1"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("http://localhost/login"));
    }

    // ---------- Registro ----------

    @Test
    void registro_datosValidos_redirigeAlLogin() throws Exception {
        mockMvc.perform(post("/registro").with(csrf())
                        .param("dni", "22222222")
                        .param("nombre", "Ana Pérez")
                        .param("password", "clave12345")
                        .param("confirmacion", "clave12345"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/login?registrado=true"));

        verify(usuarioService).registrarCliente(argThat(f -> "22222222".equals(f.getDni())));
    }

    @Test
    void registro_sinCsrf_esProhibido() throws Exception {
        mockMvc.perform(post("/registro")
                        .param("dni", "22222222")
                        .param("nombre", "Ana Pérez")
                        .param("password", "clave12345")
                        .param("confirmacion", "clave12345"))
                .andExpect(status().isForbidden());
    }

    @Test
    void registro_dniInvalido_vuelveAlFormulario() throws Exception {
        mockMvc.perform(post("/registro").with(csrf())
                        .param("dni", "123")
                        .param("nombre", "Ana Pérez")
                        .param("password", "clave12345")
                        .param("confirmacion", "clave12345"))
                .andExpect(status().isOk())
                .andExpect(view().name("registro"))
                .andExpect(model().attributeHasFieldErrors("registro", "dni"));

        verify(usuarioService, never()).registrarCliente(any());
    }

    @Test
    void registro_dniDuplicado_muestraErrorGlobal() throws Exception {
        when(usuarioService.registrarCliente(any()))
                .thenThrow(new IllegalStateException("Ya existe una cuenta con el DNI 11111111."));

        mockMvc.perform(post("/registro").with(csrf())
                        .param("dni", "11111111")
                        .param("nombre", "Ana Pérez")
                        .param("password", "clave12345")
                        .param("confirmacion", "clave12345"))
                .andExpect(status().isOk())
                .andExpect(view().name("registro"))
                .andExpect(model().attributeHasErrors("registro"));
    }

    // ---------- Con sesión ----------

    @Test
    @WithMockUser(username = "12345678", roles = "CLIENTE")
    void cliente_inicio_redirigeAlDashboard() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/dashboard"));
    }
}