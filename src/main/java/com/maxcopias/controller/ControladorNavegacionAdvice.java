package com.maxcopias.controller;

import com.maxcopias.model.Oferta;
import com.maxcopias.service.ServicioOferta;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class ControladorNavegacionAdvice {

    private final ServicioOferta servicioOferta;

    public ControladorNavegacionAdvice(ServicioOferta servicioOferta) {
        this.servicioOferta = servicioOferta;
    }

    @ModelAttribute("navOfertaPrincipal")
    public Oferta navOfertaPrincipal() {
        return servicioOferta.obtenerOfertaPrincipalActiva().orElse(null);
    }

    @ModelAttribute("navOfertaBadge")
    public String navOfertaBadge() {
        Oferta oferta = servicioOferta.obtenerOfertaPrincipalActiva().orElse(null);
        if (oferta == null) {
            return null;
        }

        Integer porcentaje = oferta.getPorcentajeDescuento();
        if (porcentaje != null && porcentaje > 0) {
            return porcentaje + "% OFF";
        }

        return "Nueva";
    }
}
