package com.restaurante.reservas.dto;

import com.restaurante.reservas.model.Rol;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Body para crear (POST) o editar (PUT) un usuario. Mismas validaciones que UsuarioFormDTO.
 * La regla de la contraseña (obligatoria al crear, opcional al editar, mínimo 8) vive en UsuarioService.
 */
public record UsuarioRequestDTO(

        @NotBlank(message = "El DNI es obligatorio")
        @Pattern(regexp = "\\d{8}", message = "El DNI debe tener 8 dígitos")
        String dni,

        @NotBlank(message = "El nombre es obligatorio")
        String nombre,

        @NotNull(message = "El rol es obligatorio")
        Rol rol,

        String password) {

    /** Adapta el body al formulario que espera UsuarioService; el id llega por la ruta, nunca por el body. */
    public UsuarioFormDTO toForm(Long id) {
        UsuarioFormDTO form = new UsuarioFormDTO();
        form.setId(id);
        form.setDni(dni);
        form.setNombre(nombre);
        form.setRol(rol);
        form.setPassword(password);
        return form;
    }
}