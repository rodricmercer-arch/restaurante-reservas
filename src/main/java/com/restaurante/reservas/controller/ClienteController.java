package com.restaurante.reservas.controller;

import com.restaurante.reservas.model.*;
import com.restaurante.reservas.repository.*;
import com.restaurante.reservas.service.CartaService;
import com.restaurante.reservas.service.MesaService;
import com.restaurante.reservas.service.NotificacionService;
import com.restaurante.reservas.service.ReservaService;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Controller
@RequestMapping("/cliente")
public class ClienteController {

    private final MesaService mesaService;
    private final CartaService cartaService;
    private final ReservaService reservaService;
    private final UsuarioRepository usuarioRepository;
    private final MesaRepository mesaRepository;
    private final NotificacionService notificacionService;

    public ClienteController(MesaService mesaService, CartaService cartaService, ReservaService reservaService,
                              UsuarioRepository usuarioRepository, MesaRepository mesaRepository,
                              NotificacionService notificacionService) {
        this.mesaService = mesaService;
        this.cartaService = cartaService;
        this.reservaService = reservaService;
        this.usuarioRepository = usuarioRepository;
        this.mesaRepository = mesaRepository;
        this.notificacionService = notificacionService;
    }

    private Usuario usuarioActual(Authentication auth) {
        return usuarioRepository.findByDni(auth.getName()).orElseThrow();
    }

    @GetMapping("/mesas")
    public String verMesas(Authentication auth, Model model) {
        Usuario cliente = usuarioActual(auth);
        model.addAttribute("mesas", mesaService.listarTodas());
        model.addAttribute("reservaActiva", reservaService.buscarReservaActiva(cliente).orElse(null));
        return "cliente/mesas";
    }

    @GetMapping("/reservar/{mesaId}")
    public String formularioCarta(@PathVariable Long mesaId, Model model) {
        Mesa mesa = mesaRepository.findById(mesaId).orElseThrow();
        model.addAttribute("mesa", mesa);
        model.addAttribute("principales", cartaService.principales());
        model.addAttribute("postres", cartaService.postres());
        model.addAttribute("bebidas", cartaService.bebidas());
        return "cliente/carta";
    }

    @PostMapping("/reservar")
    public String reservar(Authentication auth,
                            @RequestParam Long mesaId,
                            @RequestParam Long platoPrincipalId,
                            @RequestParam(required = false) Long platoPostreId,
                            @RequestParam(required = false) Long platoBebidaId,
                            Model model) {
        Usuario cliente = usuarioActual(auth);
        try {
            Reserva creada = reservaService.crearReserva(cliente, mesaId, platoPrincipalId, platoPostreId, platoBebidaId);
            // Aviso por Telegram: nunca lanza excepciones y el envío ocurre en otro hilo (ver NotificacionService).
            notificacionService.notificarReservaNueva(creada);
            return "redirect:/cliente/mireserva";
        } catch (ObjectOptimisticLockingFailureException e) {
            // Dos clientes reservaron la misma mesa a la vez: Hibernate detectó el cambio de @Version y revirtió al segundo.
            model.addAttribute("error", "Otro cliente reservó esta mesa hace un instante. Elige otra mesa.");
            Mesa mesa = mesaRepository.findById(mesaId).orElseThrow();
            model.addAttribute("mesa", mesa);
            model.addAttribute("principales", cartaService.principales());
            model.addAttribute("postres", cartaService.postres());
            model.addAttribute("bebidas", cartaService.bebidas());
            return "cliente/carta";
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            Mesa mesa = mesaRepository.findById(mesaId).orElseThrow();
            model.addAttribute("mesa", mesa);
            model.addAttribute("principales", cartaService.principales());
            model.addAttribute("postres", cartaService.postres());
            model.addAttribute("bebidas", cartaService.bebidas());
            return "cliente/carta";
        }
    }

    @GetMapping("/mireserva")
    public String miReserva(Authentication auth, Model model) {
        Usuario cliente = usuarioActual(auth);
        Optional<Reserva> reserva = reservaService.buscarReservaActiva(cliente);
        model.addAttribute("reserva", reserva.orElse(null));
        return "cliente/mireserva";
    }

    @PostMapping("/mireserva/cancelar/{id}")
    public String cancelar(Authentication auth, @PathVariable Long id) {
        reservaService.cancelarReserva(id, usuarioActual(auth));
        return "redirect:/cliente/mesas";
    }
}
