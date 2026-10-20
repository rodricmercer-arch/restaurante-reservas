package com.restaurante.reservas.controller;

import com.restaurante.reservas.model.Usuario;
import com.restaurante.reservas.repository.UsuarioRepository;
import com.restaurante.reservas.service.CartaService;
import com.restaurante.reservas.service.MesaService;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.ui.Model;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;


@Controller
public class HomeController {

    private final UsuarioRepository usuarioRepository;
    private final MesaService mesaService;
    private final CartaService cartaService;

    public HomeController(UsuarioRepository usuarioRepository,
                          MesaService mesaService,
                          CartaService cartaService) {
        this.usuarioRepository = usuarioRepository;
        this.mesaService = mesaService;
        this.cartaService = cartaService;
    }

    @GetMapping("/")
    public String inicio(Authentication auth, Model model) {
        // Para un visitante sin sesión, auth llega null (o AnonymousAuthenticationToken según el contexto).
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            return "redirect:/dashboard";
        }
        model.addAttribute("mesas", mesaService.listarTodas());
        model.addAttribute("reservaActiva", null);
        return "cliente/mesas";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/carta")
    public String carta(Model model) {
        // Página pública de solo lectura: no redirige aunque haya sesión.
        model.addAttribute("principales", cartaService.principales());
        model.addAttribute("postres", cartaService.postres());
        model.addAttribute("bebidas", cartaService.bebidas());
        return "publico/carta";
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication auth) {
        Usuario usuario = usuarioRepository.findByDni(auth.getName()).orElseThrow();
        if (usuario.getRol().name().equals("ADMIN")) {
            return "redirect:/admin/dashboard";
        }
        return "redirect:/cliente/mesas";
    }
    
    @GetMapping("/403")
    public String accesoDenegado() {
        return "error/403";
    }
}
