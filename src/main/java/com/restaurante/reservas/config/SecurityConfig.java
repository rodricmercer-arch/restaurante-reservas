package com.restaurante.reservas.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.config.Customizer;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // activa @PreAuthorize en los servicios (segunda barrera, además de las rutas)
public class SecurityConfig {

    private final AppUserDetailsService userDetailsService;

    public SecurityConfig(AppUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authManager() {
        // Construido explícitamente para evitar ambigüedad entre las dos cadenas.
        return new ProviderManager(authProvider());
    }

    /**
     * CORS para Angular. Los orígenes salen de app.cors.allowed-origins (varios, separados por coma).
     * Solo se registra para /api/**; se activa únicamente en apiChain, así que webChain no cambia.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins:http://localhost:4200}") String origenes) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.stream(origenes.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    /** Cadena 1: API REST y Swagger. Stateless, sin CSRF, autenticada por JWT. */
    @Bean
    @Order(1)
    public SecurityFilterChain apiChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
            .cors(Customizer.withDefaults()) // usa el bean corsConfigurationSource; solo en esta cadena
            .csrf(csrf -> csrf.disable()) // sin cookies de sesión no hay riesgo CSRF
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/login", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/registro").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/publico/**").permitAll()
                .requestMatchers("/api/cliente/**").hasRole("CLIENTE")
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/perfil/**").hasAnyRole("CLIENTE", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/platos/**").hasAnyRole("CLIENTE", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/platos/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/platos/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/platos/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
            );
        return http.build();
    }

    /** Lee el claim "roles" del token y lo convierte en autoridades ROLE_xxx. */
    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("roles");
        authorities.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }

    /** Cadena 2: aplicación web con Thymeleaf, sesión y CSRF activo. */
    @Bean
    @Order(2)
    public SecurityFilterChain webChain(HttpSecurity http) throws Exception {
        http
            .authenticationProvider(authProvider())
            .authorizeHttpRequests(auth -> auth
                // El orden importa: Spring Security aplica la PRIMERA regla que coincide.
                .requestMatchers("/", "/login", "/registro", "/carta", "/css/**", "/js/**", "/img/**", "/error", "/403").permitAll()
                .requestMatchers("/reportes/**").hasRole("ADMIN")
                .requestMatchers("/perfil/**").hasAnyRole("CLIENTE", "ADMIN")
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/cliente/**").hasRole("CLIENTE")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("dni")
                .passwordParameter("password")
                .defaultSuccessUrl("/dashboard", true)
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")           // con CSRF activo, el logout solo funciona por POST
                .logoutSuccessUrl("/")
                .invalidateHttpSession(true)    // destruye la sesión del servidor
                .deleteCookies("JSESSIONID")    // borra la cookie de sesión del navegador
                .permitAll()
            )
            // CSRF activo: los formularios con th:action reciben el token automáticamente.
            .exceptionHandling(e -> e.accessDeniedPage("/403"));

        return http.build();
    }
}
