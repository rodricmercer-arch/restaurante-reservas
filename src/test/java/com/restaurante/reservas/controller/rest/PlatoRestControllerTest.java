package com.restaurante.reservas.controller.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurante.reservas.config.AppUserDetailsService;
import com.restaurante.reservas.config.SecurityConfig;
import com.restaurante.reservas.dto.PlatoRequestDTO;
import com.restaurante.reservas.model.CategoriaPlato;
import com.restaurante.reservas.model.Plato;
import com.restaurante.reservas.service.CartaService;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas de contrato HTTP para PlatoRestController usando MockMvc.
 * Se aísla solo la capa web (@WebMvcTest) y se mockea CartaService,
 * por lo que no se necesita base de datos ni levantar el contexto completo.
 */
@WebMvcTest(PlatoRestController.class)
@Import(SecurityConfig.class)
class PlatoRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CartaService cartaService;

     // Necesario para que el SecurityConfig real (CSRF activo, pero exento en /api/**) pueda
    // construirse dentro del contexto reducido de @WebMvcTest: SecurityConfig
    // depende de AppUserDetailsService, que aquí no se usa realmente
    // (no hay endpoints de login en este controlador), pero debe existir
    // como bean para que Spring no recurra a la configuración de seguridad
    // por defecto (que sí tiene CSRF activo).
    @MockBean
    private AppUserDetailsService appUserDetailsService;

    // apiChain usa oauth2ResourceServer: necesita un JwtDecoder. En el test se simula;
    // jwt() inyecta la autenticación directamente, sin decodificar ningún token real.
    @MockBean
    private JwtDecoder jwtDecoder;

    private static RequestPostProcessor admin() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    private static RequestPostProcessor cliente() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_CLIENTE"));
    }
    private Plato platoDeEjemplo(Long id) {
        Plato plato = new Plato("Lomo Saltado", "Plato de fondo con papas fritas",
                new BigDecimal("28.50"), CategoriaPlato.PRINCIPAL);
        plato.setId(id);
        return plato;
    }

    // ---------- GET /api/platos ----------

    @Test
    void listarPlatos_devuelveListaConStatus200() throws Exception {
        when(cartaService.todos()).thenReturn(List.of(platoDeEjemplo(1L), platoDeEjemplo(2L)));

        mockMvc.perform(get("/api/platos").with(cliente()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].nombre").value("Lomo Saltado"));

        verify(cartaService).todos();
    }

    // ---------- POST /api/platos ----------

    @Test
    void crearPlato_conDatosValidos_devuelve201() throws Exception {
        PlatoRequestDTO request = new PlatoRequestDTO(
                "Ceviche", "Pescado fresco marinado en limón",
                new BigDecimal("32.00"), CategoriaPlato.PRINCIPAL);

        Plato guardado = platoDeEjemplo(10L);
        guardado.setNombre("Ceviche");

        when(cartaService.guardar(any(Plato.class))).thenReturn(guardado);

        mockMvc.perform(post("/api/platos")
                        .with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/platos/10"))
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.nombre").value("Ceviche"));

        verify(cartaService).guardar(any(Plato.class));
    }

    // ---------- PUT /api/platos/{id} ----------

    @Test
    void actualizarPlato_existente_devuelve200() throws Exception {
        Long id = 1L;
        PlatoRequestDTO request = new PlatoRequestDTO(
                "Lomo Saltado Especial", "Con doble carne",
                new BigDecimal("35.00"), CategoriaPlato.PRINCIPAL);

        Plato existente = platoDeEjemplo(id);
        when(cartaService.buscarPorId(id)).thenReturn(Optional.of(existente));
        when(cartaService.guardar(any(Plato.class))).thenReturn(existente);

        mockMvc.perform(put("/api/platos/{id}", id)
                        .with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));

        verify(cartaService).buscarPorId(id);
        verify(cartaService).guardar(any(Plato.class));
    }

    @Test
    void actualizarPlato_inexistente_devuelve404() throws Exception {
        Long id = 999L;
        PlatoRequestDTO request = new PlatoRequestDTO(
                "No existe", "N/A", new BigDecimal("10.00"), CategoriaPlato.POSTRE);

        when(cartaService.buscarPorId(id)).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/platos/{id}", id)
                        .with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());

        verify(cartaService, never()).guardar(any(Plato.class));
    }

    // ---------- DELETE /api/platos/{id} ----------

    @Test
    void eliminarPlato_existente_devuelve204() throws Exception {
        Long id = 1L;
        when(cartaService.buscarPorId(id)).thenReturn(Optional.of(platoDeEjemplo(id)));

        mockMvc.perform(delete("/api/platos/{id}", id).with(admin()))
                .andExpect(status().isNoContent());

        verify(cartaService).eliminar(id);
    }

    @Test
    void eliminarPlato_inexistente_devuelve404() throws Exception {
        Long id = 999L;
        when(cartaService.buscarPorId(id)).thenReturn(Optional.empty());

        mockMvc.perform(delete("/api/platos/{id}", id).with(admin()))
                .andExpect(status().isNotFound());

        verify(cartaService, never()).eliminar(eq(id));
    }

    // ---------- Seguridad JWT (Fase 4) ----------

    @Test
    void sinToken_devuelve401() throws Exception {
        mockMvc.perform(get("/api/platos"))
                .andExpect(status().isUnauthorized());

        verify(cartaService, never()).todos();
    }

    @Test
    void cliente_noPuedeCrear_devuelve403() throws Exception {
        PlatoRequestDTO request = new PlatoRequestDTO(
                "Ceviche", "Pescado fresco", new BigDecimal("32.00"), CategoriaPlato.PRINCIPAL);

        mockMvc.perform(post("/api/platos")
                        .with(cliente())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(cartaService, never()).guardar(any(Plato.class));
    }

    @Test
    void cliente_noPuedeActualizar_devuelve403() throws Exception {
        PlatoRequestDTO request = new PlatoRequestDTO(
                "Ceviche", "Pescado fresco", new BigDecimal("32.00"), CategoriaPlato.PRINCIPAL);

        mockMvc.perform(put("/api/platos/{id}", 1L)
                        .with(cliente())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(cartaService, never()).guardar(any(Plato.class));
    }

    @Test
    void cliente_noPuedeEliminar_devuelve403() throws Exception {
        mockMvc.perform(delete("/api/platos/{id}", 1L).with(cliente()))
                .andExpect(status().isForbidden());

        verify(cartaService, never()).eliminar(any());
    }

    @Test
    void admin_crearPlatoInvalido_devuelve400() throws Exception {
        PlatoRequestDTO invalido = new PlatoRequestDTO("", "x", new BigDecimal("-5"), null);

        mockMvc.perform(post("/api/platos")
                        .with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalido)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.estado").value(400))
                .andExpect(jsonPath("$.errores").isNotEmpty());

        verify(cartaService, never()).guardar(any(Plato.class));
    }

    @Test
    void admin_eliminarPlatoUsado_devuelve409() throws Exception {
        Long id = 1L;
        when(cartaService.buscarPorId(id)).thenReturn(Optional.of(platoDeEjemplo(id)));
        doThrow(new IllegalStateException("No se puede eliminar el plato: ya fue usado en reservas."))
                .when(cartaService).eliminar(id);

        mockMvc.perform(delete("/api/platos/{id}", id).with(admin()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje")
                        .value("No se puede eliminar el plato: ya fue usado en reservas."));
    }
}
