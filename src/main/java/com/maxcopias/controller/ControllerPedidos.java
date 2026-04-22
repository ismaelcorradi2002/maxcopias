package com.maxcopias.controller;

import com.maxcopias.dto.FormularioPedidoCopisteria;
import com.maxcopias.model.CaraImpresion;
import com.maxcopias.model.ModoColor;
import com.maxcopias.model.TamanoPapel;
import com.maxcopias.model.TipoEncuadernacion;
import com.maxcopias.model.TipoPapel;
import com.maxcopias.model.TipoTrabajo;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequestMapping("/copisteria")
public class ControllerPedidos {

    @InitBinder("orderForm")
    public void initBinder(WebDataBinder binder) {
        binder.setDisallowedFields("files"); // Prevent binding issues with file uploads
    }

@GetMapping({"", "/pedido", "/formulario"})
    public String formulario(@ModelAttribute("orderForm") FormularioPedidoCopisteria orderForm, Model model) {
        // Pre-select defaults for smooth JS wizard progression
        orderForm.setJobType(TipoTrabajo.IMPRESION);
        orderForm.setColorMode(ModoColor.BLACK_AND_WHITE);
        orderForm.setPaperSize(TamanoPapel.A4);
        orderForm.setPrintSide(CaraImpresion.ONE_SIDED);
        orderForm.setPaperType(TipoPapel.NORMAL);
        orderForm.setBindingType(TipoEncuadernacion.SIN_ENCUADERNACION);
        orderForm.setCopies(1);
        
        // Populate enum lists for template dropdowns
        model.addAttribute("primaryJobTypes", TipoTrabajo.values());
        model.addAttribute("colorModes", ModoColor.values());
        model.addAttribute("paperSizes", TamanoPapel.values());
        model.addAttribute("printSides", CaraImpresion.values());
        model.addAttribute("paperTypes", TipoPapel.values());
        model.addAttribute("bindingTypes", TipoEncuadernacion.values());
        
        // Static preview and config
        model.addAttribute("acceptedFormats", "PDF, JPG, JPEG, PNG");
        model.addAttribute("maxFileSizeLabel", "15 MB");
        
        // Empty price preview (will be populated by JS/service)
        model.addAttribute("pricePreview", new com.maxcopias.model.EstimacionPrecioCopisteria(
            java.math.BigDecimal.ZERO, 
            "Selecciona un servicio para ver el precio orientativo del pedido.", 
            "El importe se calcula automaticamente segun la configuracion y los archivos.", 
            0, 0
        ));
        
        return "copisteria/formulario";
    }

    @PostMapping
    public String procesarPedido(
            @Valid @ModelAttribute("orderForm") FormularioPedidoCopisteria orderForm,
            BindingResult result,
            @RequestParam("files") MultipartFile[] files,
            Model model) {
        
        if (result.hasErrors() || files == null || files.length == 0) {
            // Re-populate model attributes on validation error
            formulario(orderForm, model);
            return "copisteria/formulario";
        }
        
        // TODO: Process files, save order, generate price
        // For now, redirect to summary
        return "redirect:/copisteria/resumen";
    }

    @GetMapping("/mis-pedidos")
    public String misPedidos() {
        return "area-personal/pedidos";
    }
}
