package com.restaurante.reservas.controller.rest;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

/**
 * Convierte excepciones de la API REST en respuestas JSON uniformes.
 * Limitado al paquete controller.rest: no afecta a los controladores Thymeleaf.
 */
@RestControllerAdvice(basePackages = "com.restaurante.reservas.controller.rest")
public class RestExceptionHandler {

    /** Formato único de error: {estado, mensaje, errores}. */
    public record ErrorRespuesta(int estado, String mensaje, List<String> errores) {
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorRespuesta> validacion(MethodArgumentNotValidException ex) {
        List<String> errores = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .toList();
        return ResponseEntity.badRequest()
                .body(new ErrorRespuesta(400, "Datos inválidos", errores));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorRespuesta> credenciales(BadCredentialsException ex) {
        return ResponseEntity.status(401)
                .body(new ErrorRespuesta(401, "DNI o contraseña incorrectos", List.of()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorRespuesta> conflicto(IllegalStateException ex) {
        return ResponseEntity.status(409)
                .body(new ErrorRespuesta(409, ex.getMessage(), List.of()));
    }
}