package com.restaurante.reservas.controller;

import com.restaurante.reservas.model.*;
import com.restaurante.reservas.repository.UsuarioRepository;
import com.restaurante.reservas.service.CartaService;
import com.restaurante.reservas.service.MesaService;
import com.restaurante.reservas.service.ReservaService;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import java.math.BigDecimal;
import java.time.LocalDate;


@Controller
@RequestMapping("/admin")
public class AdminController {

    private final MesaService mesaService;
    private final CartaService cartaService;
    private final ReservaService reservaService;
    private final UsuarioRepository usuarioRepository;

    public AdminController(MesaService mesaService, CartaService cartaService,
                            ReservaService reservaService, UsuarioRepository usuarioRepository) {
        this.mesaService = mesaService;
        this.cartaService = cartaService;
        this.reservaService = reservaService;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication auth, Model model) {
        model.addAttribute("admin", usuarioRepository.findByDni(auth.getName()).orElseThrow());
        model.addAttribute("totalMesas", mesaService.listarTodas().size());
        model.addAttribute("totalReservas", reservaService.listarTodas().size());
        return "admin/dashboard";
    }

    // ---- Mesas ----
    @GetMapping("/mesas")
    public String mesas(Model model) {
        model.addAttribute("mesas", mesaService.listarTodas());
        model.addAttribute("mesaNueva", new Mesa());
        return "admin/mesas";
    }

    @GetMapping("/mesas/nueva")
    public String nuevaMesa(Model model) {
        model.addAttribute("mesa", new Mesa());
        return "admin/mesa-form";
    }

    @GetMapping("/mesas/editar/{id}")
    public String editarMesa(@PathVariable Long id, Model model, RedirectAttributes ra) {
        Mesa mesa = mesaService.buscarPorId(id).orElse(null);
        if (mesa == null) {
            ra.addFlashAttribute("error", "La mesa no existe.");
            return "redirect:/admin/mesas";
        }
        model.addAttribute("mesa", mesa);
        return "admin/mesa-form";
    }

    @PostMapping("/mesas/guardar")
    public String guardarMesa(@Valid @ModelAttribute("mesa") Mesa mesa,
                              BindingResult result, RedirectAttributes ra) {
        if (result.hasErrors()) {
            return "admin/mesa-form";
        }
        try {
            if (mesa.getId() == null) {
                mesaService.crear(mesa);
                ra.addFlashAttribute("mensaje", "Mesa creada correctamente.");
            } else {
                mesaService.actualizar(mesa.getId(), mesa);
                ra.addFlashAttribute("mensaje", "Mesa actualizada correctamente.");
            }
        } catch (IllegalStateException e) {
            result.rejectValue("numero", "duplicado", e.getMessage());
            return "admin/mesa-form";
        }
        return "redirect:/admin/mesas";
    }

    @PostMapping("/mesas/eliminar/{id}")
    public String eliminarMesa(@PathVariable Long id, RedirectAttributes ra) {
        try {
            mesaService.eliminar(id);
            ra.addFlashAttribute("mensaje", "Mesa eliminada correctamente.");
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/mesas";
    }

    // ---- Carta / Platos ----
    @GetMapping("/platos")
    public String platos(@RequestParam(required = false) String texto,
                         @RequestParam(required = false) CategoriaPlato categoria,
                         @RequestParam(required = false) BigDecimal precioMin,
                         @RequestParam(required = false) BigDecimal precioMax,
                         Model model) {
        // El servicio normaliza los nulos; aquí solo se pasan los valores crudos del formulario.
        model.addAttribute("platos", cartaService.buscarAvanzado(texto, categoria, precioMin, precioMax));
        model.addAttribute("categorias", CategoriaPlato.values());

        // Se devuelven los filtros para que el formulario conserve lo que el usuario escribió.
        model.addAttribute("texto", texto);
        model.addAttribute("categoria", categoria);
        model.addAttribute("precioMin", precioMin);
        model.addAttribute("precioMax", precioMax);
        return "admin/platos";
    }


    @GetMapping("/platos/nuevo")
    public String nuevoPlato(Model model) {
        model.addAttribute("plato", new Plato());
        model.addAttribute("categorias", CategoriaPlato.values());
        return "admin/plato-form";
    }

    @GetMapping("/platos/editar/{id}")
    public String editarPlato(@PathVariable Long id, Model model, RedirectAttributes ra) {
        Plato plato = cartaService.buscarPorId(id).orElse(null);
        if (plato == null) {
            ra.addFlashAttribute("error", "El plato no existe.");
            return "redirect:/admin/platos";
        }
        model.addAttribute("plato", plato);
        model.addAttribute("categorias", CategoriaPlato.values());
        return "admin/plato-form";
    }

    @PostMapping("/platos/guardar")
    public String guardarPlato(@Valid @ModelAttribute("plato") Plato plato,
                               BindingResult result, Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            model.addAttribute("categorias", CategoriaPlato.values());
            return "admin/plato-form";
        }
        try {
            if (plato.getId() == null) {
                cartaService.crear(plato);
                ra.addFlashAttribute("mensaje", "Plato creado correctamente.");
            } else {
                cartaService.actualizar(plato.getId(), plato);
                ra.addFlashAttribute("mensaje", "Plato actualizado correctamente.");
            }
        } catch (IllegalStateException e) {
            result.rejectValue("nombre", "duplicado", e.getMessage());
            model.addAttribute("categorias", CategoriaPlato.values());
            return "admin/plato-form";
        }
        return "redirect:/admin/platos";
    }

    @PostMapping("/platos/eliminar/{id}")
    public String eliminarPlato(@PathVariable Long id, RedirectAttributes ra) {
        try {
            cartaService.eliminar(id);
            ra.addFlashAttribute("mensaje", "Plato eliminado correctamente.");
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/platos";
    }

    // ---- Reservas ----
    @GetMapping("/reservas")
    public String reservas(@RequestParam(required = false) String dni,
                           @RequestParam(required = false) EstadoReserva estado,
                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                           Model model) {
        model.addAttribute("reservas", reservaService.buscarConFiltros(dni, estado, desde, hasta));
        model.addAttribute("mesasLibres", mesaService.listarTodas().stream()
                .filter(m -> m.getEstado() == EstadoMesa.LIBRE).toList());
        model.addAttribute("estados", EstadoReserva.values());

        // Se devuelven los filtros para que el formulario conserve lo que el usuario eligió.
        model.addAttribute("dni", dni);
        model.addAttribute("estado", estado);
        model.addAttribute("desde", desde);
        model.addAttribute("hasta", hasta);
        return "admin/reservas";
    }

    @PostMapping("/reservas/cancelar/{id}")
    public String cancelarReserva(Authentication auth, @PathVariable Long id, RedirectAttributes ra) {
        try {
            reservaService.cancelarReserva(id, usuarioRepository.findByDni(auth.getName()).orElseThrow());
            ra.addFlashAttribute("mensaje", "Reserva cancelada correctamente.");
        } catch (IllegalStateException | IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/reservas";
    }

    @PostMapping("/reservas/mover/{id}")
    public String moverReserva(@PathVariable Long id, @RequestParam Long nuevaMesaId, RedirectAttributes ra) {
        try {
            reservaService.moverReserva(id, nuevaMesaId);
            ra.addFlashAttribute("mensaje", "Reserva movida correctamente.");
        } catch (ObjectOptimisticLockingFailureException e) {
            // Otra persona modificó una de las mesas entre que se mostró la pantalla y se pulsó "Mover".
            ra.addFlashAttribute("error", "Otra persona modificó una de las mesas al mismo tiempo. Intenta de nuevo.");
        } catch (IllegalStateException | IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/reservas";
    }
}
