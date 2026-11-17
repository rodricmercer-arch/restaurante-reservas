package com.restaurante.reservas.controller.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurante.reservas.config.AppUserDetailsService;
import com.restaurante.reservas.config.SecurityConfig;
import com.restaurante.reservas.dto.CambiarPasswordRequestDTO;
import com.restaurante.reservas.dto.RegistroFormDTO;
import com.restaurante.reservas.model.Rol;
import com.restaurante.reservas.model.Usuario;
import com.restaurante.reservas.repository.ReservaRepository;
import com.restaurante.reservas.repository.UsuarioRepository;
import com.restaurante.reservas.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas de contrato HTTP de RegistroRestController, AdminUsuarioRestController y PerfilRestController.
 * Solo capa web: los servicios y repositorios están simulados, no hace falta base de datos.
 */
@WebMvcTest({RegistroRestController.class, AdminUsuarioRestController.class, PerfilRestController.class})
@Import(SecurityConfig.class)
class UsuarioPerfilRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UsuarioService usuarioService;

    @MockBean
    private UsuarioRepository usuarioRepository;

    @MockBean
    private ReservaRepository reservaRepository;

    // Necesarios para que SecurityConfig se construya en el contexto reducido (igual que en PlatoRestControllerTest).
    @MockBean
    private AppUserDetailsService appUserDetailsService;

    @MockBean
    private JwtDecoder jwtDecoder;

    private static RequestPostProcessor admin() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    private static RequestPostProcessor cliente() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_CLIENTE"));
    }

    /** Token con un DNI concreto como subject (el controlador lo lee con jwt.getSubject()). */
    private static RequestPostProcessor conDni(String dni, String authority) {
        return jwt().jwt(j -> j.subject(dni)).authorities(new SimpleGrantedAuthority(authority));
    }

    private RegistroFormDTO registro(String dni) {
        RegistroFormDTO f = new RegistroFormDTO();
        f.setDni(dni);
        f.setNombre("Ana Pérez");
        f.setPassword("clave12345");
        f.setConfirmacion("clave12345");
        return f;
    }

    // ---------- POST /api/auth/registro (público) ----------

    @Test
    void registro_datosValidos_devuelve201SinPassword() throws Exception {
        Usuario creado = new Usuario("71234567", "hash-bcrypt", "Ana Pérez", Rol.CLIENTE);
        when(usuarioService.registrarCliente(any(RegistroFormDTO.class))).thenReturn(creado);

        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registro("71234567"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.dni").value("71234567"))
                .andExpect(jsonPath("$.nombre").value("Ana Pérez"))
                .andExpect(jsonPath("$.password").doesNotExist());

        verify(usuarioService).registrarCliente(any(RegistroFormDTO.class));
    }

    @Test
    void registro_dniDuplicado_devuelve409() throws Exception {
        when(usuarioService.registrarCliente(any(RegistroFormDTO.class)))
                .thenThrow(new IllegalStateException("Ya existe una cuenta con el DNI 71234567."));

        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registro("71234567"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("Ya existe una cuenta con el DNI 71234567."));
    }

    @Test
    void registro_dniInvalido_devuelve400() throws Exception {
        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registro("123"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.estado").value(400))
                .andExpect(jsonPath("$.errores").isNotEmpty());

        verify(usuarioService, never()).registrarCliente(any(RegistroFormDTO.class));
    }

    // ---------- /api/admin/usuarios (solo ADMIN) ----------

    @Test
    void cliente_noPuedeListarUsuarios_devuelve403() throws Exception {
        mockMvc.perform(get("/api/admin/usuarios").with(cliente()))
                .andExpect(status().isForbidden());

        verify(usuarioService, never()).listar();
    }

    @Test
    void sinToken_listarUsuarios_devuelve401() throws Exception {
        mockMvc.perform(get("/api/admin/usuarios"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void admin_listarUsuarios_devuelve200SinPassword() throws Exception {
        Usuario u = new Usuario("00000000", "hash-secreto", "Administrador", Rol.ADMIN);
        u.setId(1L);
        when(usuarioService.listar()).thenReturn(List.of(u));

        mockMvc.perform(get("/api/admin/usuarios").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].dni").value("00000000"))
                .andExpect(jsonPath("$[0].rol").value("ADMIN"))
                .andExpect(jsonPath("$[0].password").doesNotExist());
    }

    @Test
    void admin_eliminarSuPropiaCuenta_devuelve409() throws Exception {
        Usuario yo = new Usuario("00000000", "x", "Administrador", Rol.ADMIN);
        yo.setId(5L);
        when(usuarioService.buscarPorId(5L)).thenReturn(Optional.of(yo));
        doThrow(new IllegalStateException("No puede eliminar su propia cuenta."))
                .when(usuarioService).eliminar(5L, "00000000");

        mockMvc.perform(delete("/api/admin/usuarios/{id}", 5L).with(conDni("00000000", "ROLE_ADMIN")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("No puede eliminar su propia cuenta."));
    }

    // ---------- /api/perfil ----------

    @Test
    void perfil_cambiarPasswordConActualIncorrecta_devuelve400() throws Exception {
        doThrow(new IllegalStateException("La contraseña actual es incorrecta."))
                .when(usuarioService).cambiarPassword("71234567", "incorrecta", "nuevaClave123", "nuevaClave123");

        mockMvc.perform(put("/api/perfil/password")
                        .with(conDni("71234567", "ROLE_CLIENTE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CambiarPasswordRequestDTO("incorrecta", "nuevaClave123", "nuevaClave123"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.estado").value(400))
                .andExpect(jsonPath("$.mensaje").value("La contraseña actual es incorrecta."));
    }

    @Test
    void perfil_cambiarPasswordCorrecto_devuelve204() throws Exception {
        mockMvc.perform(put("/api/perfil/password")
                        .with(conDni("71234567", "ROLE_CLIENTE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CambiarPasswordRequestDTO("clave12345", "nuevaClave123", "nuevaClave123"))))
                .andExpect(status().isNoContent());

        verify(usuarioService).cambiarPassword("71234567", "clave12345", "nuevaClave123", "nuevaClave123");
    }

    @Test
    void perfil_admin_noIncluyeTotalReservas() throws Exception {
        Usuario admin = new Usuario("00000000", "x", "Administrador", Rol.ADMIN);
        admin.setId(1L);
        when(usuarioRepository.findByDni("00000000")).thenReturn(Optional.of(admin));

        mockMvc.perform(get("/api/perfil").with(conDni("00000000", "ROLE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dni").value("00000000"))
                .andExpect(jsonPath("$.rol").value("ADMIN"))
                .andExpect(jsonPath("$.totalReservas").doesNotExist());

        verifyNoInteractions(reservaRepository);
    }
}