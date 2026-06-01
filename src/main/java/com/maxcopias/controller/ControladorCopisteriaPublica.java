package com.maxcopias.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ControladorCopisteriaPublica {

    @GetMapping("/copisteria")
    public String landingCopisteria() {
        return "copisteria/landing";
    }
}
