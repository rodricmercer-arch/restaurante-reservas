package com.restaurante.reservas.controller;

import com.restaurante.reservas.model.Rol;
import com.restaurante.reservas.model.Usuario;
import com.restaurante.reservas.repository.ReservaRepository;
import com.restaurante.reservas.repository.UsuarioRepository;
import com.restaurante.reservas.service.UsuarioService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/perfil")
public class PerfilController {

    private final UsuarioRepository usuarioRepository;
    private final ReservaRepository reservaRepository;
    private final UsuarioService usuarioService;

    public PerfilController(UsuarioRepository usuarioRepository,
                            ReservaRepository reservaRepository,
                            UsuarioService usuarioService) {
        this.usuarioRepository = usuarioRepository;
        this.reservaRepository = reservaRepository;
        this.usuarioService = usuarioService;
    }

    @GetMapping("")
    public String perfil(Authentication auth, Model model) {
        Usuario usuario = usuarioRepository.findByDni(auth.getName()).orElseThrow();
        model.addAttribute("usuario", usuario);
        // Solo el CLIENTE tiene reservas propias que mostrar.
        if (usuario.getRol() == Rol.CLIENTE) {
            model.addAttribute("totalReservas", reservaRepository.countByClienteId(usuario.getId()));
        }
        return "perfil/index";
    }

    @GetMapping("/password")
    public String formularioPassword() {
        return "perfil/password";
    }

    @PostMapping("/password")
    public String cambiarPassword(Authentication auth,
                                  @RequestParam String actual,
                                  @RequestParam String nueva,
                                  @RequestParam String confirmacion,
                                  RedirectAttributes ra) {
        try {
            // El DNI sale de la sesión, nunca del formulario: nadie puede cambiar la clave de otro.
            usuarioService.cambiarPassword(auth.getName(), actual, nueva, confirmacion);
            ra.addFlashAttribute("mensaje", "Contraseña actualizada correctamente.");
            return "redirect:/perfil";
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/perfil/password";
        }
    }
}