package com.restaurante.reservas.controller;

import com.restaurante.reservas.service.ReporteService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/reportes")
public class ReporteController {

    private final ReporteService reporteService;

    public ReporteController(ReporteService reporteService) {
        this.reporteService = reporteService;
    }

    @GetMapping("")
    public String index(Model model) {
        model.addAttribute("reporte", reporteService.generar());
        return "reportes/index";
    }
}