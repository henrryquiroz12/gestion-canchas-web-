package com.canchas.web.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller	@RequiredArgsConstructor
public class PublicAboutController {

    @GetMapping("/nosotros")
    public String showAbout(Model model) {
        model.addAttribute("activeSection", "nosotros");
        return "nosotros";
    }
}
