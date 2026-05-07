package com.maxcopias.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ControladorLegal {

    @GetMapping("/aviso-legal")
    public String avisoLegal() {
        return "legal/aviso-legal";
    }

    @GetMapping("/privacidad")
    public String privacidad() {
        return "legal/privacidad";
    }

    @GetMapping("/cookies")
    public String cookies() {
        return "legal/cookies";
    }

    @GetMapping("/condiciones-compra")
    public String condicionesCompra() {
        return "legal/condiciones-compra";
    }
}
