package com.canchas.web.controller;

import com.canchas.web.service.CourtService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final CourtService courtService;

    @GetMapping("/")
    public String root() {
        return "redirect:/inicio";
    }

    @GetMapping("/inicio")
    public String showLanding(Model model) {
        model.addAttribute("totalCanchas", courtService.findAvailable().size());
        model.addAttribute("activeSection", "inicio");
        return "inicio";
    }

    @GetMapping("/como-funciona")
    public String howItWorks() {
        return "redirect:/inicio#como-reservar";
    }
}
