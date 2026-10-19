package com.restaurante.reservas.controller.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurante.reservas.config.AppUserDetailsService;
import com.restaurante.reservas.config.SecurityConfig;
import com.restaurante.reservas.dto.LoginRequestDTO;
import com.restaurante.reservas.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthRestController.class)
@Import(SecurityConfig.class)
class AuthRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // Reemplaza el bean authManager de SecurityConfig: aquí no se prueba el BCrypt, solo el contrato HTTP.
    @MockBean(name = "authManager")
    private AuthenticationManager authenticationManager;

    @MockBean
    private JwtService jwtService;

    // Necesarios para que SecurityConfig se construya en el contexto reducido.
    @MockBean
    private JwtDecoder jwtDecoder;

    @MockBean
    private AppUserDetailsService appUserDetailsService;

    @Test
    void login_correcto_devuelve200ConToken() throws Exception {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "00000000", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(auth);
        when(jwtService.generarToken(any(Authentication.class))).thenReturn("token-de-prueba");
        when(jwtService.getExpiracionSegundos()).thenReturn(3600L);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequestDTO("00000000", "admin123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("token-de-prueba"))
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.expiraEnSegundos").value(3600))
                .andExpect(jsonPath("$.dni").value("00000000"))
                .andExpect(jsonPath("$.rol").value("ADMIN"));
    }

    @Test
    void login_credencialesInvalidas_devuelve401() throws Exception {
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequestDTO("00000000", "incorrecta"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.estado").value(401));
    }
}