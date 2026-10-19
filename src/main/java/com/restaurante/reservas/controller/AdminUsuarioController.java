package com.restaurante.reservas.controller;

import com.restaurante.reservas.dto.UsuarioFormDTO;
import com.restaurante.reservas.model.Rol;
import com.restaurante.reservas.model.Usuario;
import com.restaurante.reservas.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/usuarios")
public class AdminUsuarioController {

    private final UsuarioService usuarioService;

    public AdminUsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public String lista(Model model) {
        model.addAttribute("usuarios", usuarioService.listar());
        return "admin/usuarios";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("usuario", new UsuarioFormDTO());
        model.addAttribute("roles", Rol.values());
        return "admin/usuario-form";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Long id, Model model, RedirectAttributes ra) {
        Usuario u = usuarioService.buscarPorId(id).orElse(null);
        if (u == null) {
            ra.addFlashAttribute("error", "El usuario no existe.");
            return "redirect:/admin/usuarios";
        }
        UsuarioFormDTO form = new UsuarioFormDTO();
        form.setId(u.getId());
        form.setDni(u.getDni());
        form.setNombre(u.getNombre());
        form.setRol(u.getRol());
        model.addAttribute("usuario", form);
        model.addAttribute("roles", Rol.values());
        return "admin/usuario-form";
    }

    @PostMapping("/guardar")
    public String guardar(@Valid @ModelAttribute("usuario") UsuarioFormDTO form,
                          BindingResult result, Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            model.addAttribute("roles", Rol.values());
            return "admin/usuario-form";
        }
        try {
            if (form.getId() == null) {
                usuarioService.crear(form);
                ra.addFlashAttribute("mensaje", "Usuario creado correctamente.");
            } else {
                usuarioService.actualizar(form);
                ra.addFlashAttribute("mensaje", "Usuario actualizado correctamente.");
            }
        } catch (IllegalStateException e) {
            result.reject("error.servicio", e.getMessage());
            model.addAttribute("roles", Rol.values());
            return "admin/usuario-form";
        }
        return "redirect:/admin/usuarios";
    }

    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        try {
            usuarioService.eliminar(id, auth.getName());
            ra.addFlashAttribute("mensaje", "Usuario eliminado correctamente.");
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/usuarios";
    }
}