package com.maxcopias.controller;

/**
 * Controlador del área personal del usuario (perfil, pedidos).
 */
import com.maxcopias.dto.FormularioActualizarPerfil;
import com.maxcopias.service.ServicioPedidoCopisteria;
import com.maxcopias.model.Usuario;
import com.maxcopias.service.ServicioUsuario;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ControladorAreaPersonal {

    private final ServicioUsuario userService;
    private final ServicioPedidoCopisteria copisteriaOrderService;

    /**
     * Inyecta servicios de usuario y pedidos.
     */
    public ControladorAreaPersonal(ServicioUsuario userService, ServicioPedidoCopisteria copisteriaOrderService) {
        this.userService = userService;
        this.copisteriaOrderService = copisteriaOrderService;
    }

    /**
     * Muestra página área personal con formulario perfil.
     */
    @GetMapping("/area-personal")
    public String personalArea(
        Authentication authentication,
        Model model,
        @RequestParam(name = "updated", defaultValue = "false") boolean updated
    ) {
        Usuario currentUsuario = userService.findRequiredByEmail(authentication.getName());
        populatePersonalAreaModel(model, currentUsuario, FormularioActualizarPerfil.fromUsuario(currentUsuario), updated);
        return "area-personal/inicio";
    }

    /**
     * Actualiza perfil del usuario.
     */
    @PostMapping("/area-personal")
    public String updatePersonalArea(
        Authentication authentication,
        @Valid @ModelAttribute("profileForm") FormularioActualizarPerfil profileForm,
        BindingResult bindingResult,
        Model model
    ) {
        Usuario currentUsuario = userService.findRequiredByEmail(authentication.getName());

        if (bindingResult.hasErrors()) {
            populatePersonalAreaModel(model, currentUsuario, profileForm, false);
            return "area-personal/inicio";
        }

        userService.updateProfile(authentication.getName(), profileForm);
        return "redirect:/area-personal?updated";
    }

    @GetMapping("/dashboard")
    public String legacyDashboardRoute() {
        return "redirect:/area-personal";
    }

    /**
     * Lista pedidos del usuario.
     */
    @GetMapping("/mis-pedidos")
    public String myOrders(Authentication authentication, Model model) {
        Usuario currentUsuario = userService.findRequiredByEmail(authentication.getName());
        model.addAttribute("currentUsuario", currentUsuario);
        model.addAttribute("orders", copisteriaOrderService.findOrdersForUsuario(currentUsuario));
        model.addAttribute("pageTitle", "Maxcopias | Mis pedidos");
        return "area-personal/pedidos";
    }

    @GetMapping("/pedido")
    public String orderShortcut() {
        return "redirect:/copisteria/pedido";
    }

    /**
     * Página admin (redirigido desde seguridad).
     */
    @GetMapping("/admin")
    public String admin(Authentication authentication, Model model) {
        Usuario currentUsuario = userService.findRequiredByEmail(authentication.getName());
        model.addAttribute("currentUsuario", currentUsuario);
        return "administracion/inicio";
    }

    private void populatePersonalAreaModel(Model model, Usuario currentUsuario, FormularioActualizarPerfil profileForm, boolean updated) {
        model.addAttribute("currentUsuario", currentUsuario);
        model.addAttribute("profileForm", profileForm);
        model.addAttribute("profileUpdated", updated);
        model.addAttribute("pageTitle", "Maxcopias | Area personal");
    }
}

