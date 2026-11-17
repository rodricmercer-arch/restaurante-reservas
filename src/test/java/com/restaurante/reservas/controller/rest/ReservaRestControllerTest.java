package com.restaurante.reservas.controller.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurante.reservas.config.AppUserDetailsService;
import com.restaurante.reservas.config.SecurityConfig;
import com.restaurante.reservas.dto.CrearReservaRequestDTO;
import com.restaurante.reservas.model.CategoriaPlato;
import com.restaurante.reservas.model.DetalleReserva;
import com.restaurante.reservas.model.Mesa;
import com.restaurante.reservas.model.Plato;
import com.restaurante.reservas.model.Reserva;
import com.restaurante.reservas.model.Rol;
import com.restaurante.reservas.model.Usuario;
import com.restaurante.reservas.repository.UsuarioRepository;
import com.restaurante.reservas.service.NotificacionService;
import com.restaurante.reservas.service.ReservaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas de contrato HTTP de ClienteRestController y AdminReservaRestController (MockMvc).
 * Solo capa web: servicios y repositorio simulados, sin base de datos.
 */
@WebMvcTest({ClienteRestController.class, AdminReservaRestController.class})
@Import(SecurityConfig.class)
class ReservaRestControllerTest {

    private static final String DNI = "71234567";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ReservaService reservaService;

    @MockBean
    private NotificacionService notificacionService;

    @MockBean
    private UsuarioRepository usuarioRepository;

    // Necesarios para que SecurityConfig se construya en el contexto reducido (igual que en PlatoRestControllerTest).
    @MockBean
    private AppUserDetailsService appUserDetailsService;

    @MockBean
    private JwtDecoder jwtDecoder;

    /** Token CLIENTE con el DNI como subject (los controladores lo leen con jwt.getSubject()). */
    private static RequestPostProcessor cliente() {
        return jwt().jwt(j -> j.subject(DNI)).authorities(new SimpleGrantedAuthority("ROLE_CLIENTE"));
    }

    private Usuario clienteEntidad() {
        Usuario u = new Usuario(DNI, "hash-secreto", "Ana Pérez", Rol.CLIENTE);
        u.setId(1L);
        return u;
    }

    private Reserva reservaDeEjemplo() {
        Mesa mesa = new Mesa(5, 4);
        mesa.setId(10L);
        Plato principal = new Plato("Lomo Saltado", "Con papas fritas",
                new BigDecimal("28.50"), CategoriaPlato.PRINCIPAL);
        principal.setId(1L);

        Reserva reserva = new Reserva();
        reserva.setId(100L);
        reserva.setCliente(clienteEntidad());
        reserva.setMesa(mesa);
        reserva.addDetalle(new DetalleReserva(principal, 1));
        return reserva;
    }

    private String cuerpoReserva(Long mesaId) throws Exception {
        return objectMapper.writeValueAsString(new CrearReservaRequestDTO(mesaId, 1L, null, null));
    }

    // ---------- Sin token ----------

    @Test
    void sinToken_devuelve401() throws Exception {
        mockMvc.perform(post("/api/cliente/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoReserva(10L)))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(reservaService);
    }

    // ---------- POST /api/cliente/reservas ----------

    @Test
    void cliente_creaReserva_devuelve201() throws Exception {
        when(usuarioRepository.findByDni(DNI)).thenReturn(Optional.of(clienteEntidad()));
        when(reservaService.crearReserva(any(Usuario.class), eq(10L), eq(1L), isNull(), isNull()))
                .thenReturn(reservaDeEjemplo());

        mockMvc.perform(post("/api/cliente/reservas")
                        .with(cliente())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoReserva(10L)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/cliente/reserva-activa"))
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.estado").value("CONFIRMADA"))
                .andExpect(jsonPath("$.mesa.numero").value(5))
                .andExpect(jsonPath("$.cliente.dni").value(DNI))
                .andExpect(jsonPath("$.cliente.password").doesNotExist())
                .andExpect(jsonPath("$.detalles.length()").value(1))
                .andExpect(jsonPath("$.detalles[0].nombre").value("Lomo Saltado"));

        verify(notificacionService).notificarReservaNueva(any(Reserva.class));
    }

    @Test
    void cliente_segundaReservaActiva_devuelve409() throws Exception {
        when(usuarioRepository.findByDni(DNI)).thenReturn(Optional.of(clienteEntidad()));
        when(reservaService.crearReserva(any(Usuario.class), eq(10L), eq(1L), isNull(), isNull()))
                .thenThrow(new IllegalStateException("Ya tienes una reserva activa."));

        mockMvc.perform(post("/api/cliente/reservas")
                        .with(cliente())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoReserva(10L)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.estado").value(409))
                .andExpect(jsonPath("$.mensaje").value("Ya tienes una reserva activa."));

        verify(notificacionService, never()).notificarReservaNueva(any());
    }

    @Test
    void cliente_mesaInexistente_devuelve400() throws Exception {
        when(usuarioRepository.findByDni(DNI)).thenReturn(Optional.of(clienteEntidad()));
        when(reservaService.crearReserva(any(Usuario.class), eq(9999L), eq(1L), isNull(), isNull()))
                .thenThrow(new IllegalArgumentException("La mesa no existe."));

        mockMvc.perform(post("/api/cliente/reservas")
                        .with(cliente())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoReserva(9999L)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.estado").value(400))
                .andExpect(jsonPath("$.mensaje").value("La mesa no existe."));

        verify(notificacionService, never()).notificarReservaNueva(any());
    }

    @Test
    void cliente_mesaTomadaALaVez_devuelve409() throws Exception {
        when(usuarioRepository.findByDni(DNI)).thenReturn(Optional.of(clienteEntidad()));
        when(reservaService.crearReserva(any(Usuario.class), eq(10L), eq(1L), isNull(), isNull()))
                .thenThrow(new ObjectOptimisticLockingFailureException(Mesa.class, 10L));

        mockMvc.perform(post("/api/cliente/reservas")
                        .with(cliente())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoReserva(10L)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value(
                        "El recurso fue modificado por otra persona hace un instante. Intenta de nuevo o elige otra mesa."));
    }

    // ---------- CLIENTE llamando a /api/admin/** ----------

    @Test
    void cliente_noPuedeListarReservasAdmin_devuelve403() throws Exception {
        mockMvc.perform(get("/api/admin/reservas").with(cliente()))
                .andExpect(status().isForbidden());

        verify(reservaService, never()).buscarConFiltros(any(), any(), any(), any());
    }

    @Test
    void cliente_noPuedeCancelarComoAdmin_devuelve403() throws Exception {
        mockMvc.perform(post("/api/admin/reservas/{id}/cancelar", 1L).with(cliente()))
                .andExpect(status().isForbidden());

        verify(reservaService, never()).cancelarReserva(any(), any());
    }

    @Test
    void cliente_noPuedeVerDashboard_devuelve403() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard").with(cliente()))
                .andExpect(status().isForbidden());
    }
}