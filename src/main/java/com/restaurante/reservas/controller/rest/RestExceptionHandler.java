package com.restaurante.reservas.controller.rest;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
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

    /** Datos que no existen o no son válidos (ej. "La mesa no existe."). */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorRespuesta> argumentoInvalido(IllegalArgumentException ex) {
        return ResponseEntity.badRequest()
                .body(new ErrorRespuesta(400, ex.getMessage(), List.of()));
    }

    /** Dos personas modificaron la misma mesa a la vez (@Version de Mesa). */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorRespuesta> bloqueoOptimista(ObjectOptimisticLockingFailureException ex) {
        return ResponseEntity.status(409)
                .body(new ErrorRespuesta(409,
                        "El recurso fue modificado por otra persona hace un instante. Intenta de nuevo o elige otra mesa.",
                        List.of()));
    }

    /** JSON mal formado, tipo de dato incorrecto o enum desconocido. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorRespuesta> cuerpoIlegible(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest()
                .body(new ErrorRespuesta(400, "Cuerpo de la petición inválido", List.of()));
    }
}