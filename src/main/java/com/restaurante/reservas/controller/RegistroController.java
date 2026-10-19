package com.restaurante.reservas.controller;

import com.restaurante.reservas.dto.RegistroFormDTO;
import com.restaurante.reservas.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class RegistroController {

    private final UsuarioService usuarioService;

    public RegistroController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/registro")
    public String formulario(Authentication auth, Model model) {
        if (haySesion(auth)) {
            return "redirect:/dashboard";
        }
        model.addAttribute("registro", new RegistroFormDTO());
        return "registro";
    }

    @PostMapping("/registro")
    public String registrar(@Valid @ModelAttribute("registro") RegistroFormDTO form,
                            BindingResult result) {
        if (result.hasErrors()) {
            return "registro";
        }
        try {
            usuarioService.registrarCliente(form);
        } catch (IllegalStateException e) {
            // Error global (no pertenece a un campo): se muestra con #fields.globalErrors() en la vista.
            result.reject("registro.error", e.getMessage());
            return "registro";
        }
        return "redirect:/login?registrado=true";
    }

    private boolean haySesion(Authentication auth) {
        return auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken);
    }
}