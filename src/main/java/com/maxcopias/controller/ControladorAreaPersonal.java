package com.maxcopias.controller;

/**
 * Controlador del área personal del usuario (perfil, pedidos).
 */
import com.maxcopias.dto.FormularioActualizarPerfil;
import com.maxcopias.service.ServicioPedidoCopisteria;
import com.maxcopias.model.Usuario;
import com.maxcopias.service.ServicioUsuario;
import com.maxcopias.model.Rol;
import com.maxcopias.repository.RepositorioUsuario;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;
import java.util.Optional;

import com.maxcopias.model.Producto;
import com.maxcopias.service.ServicioTienda;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class ControladorAreaPersonal {

    private final ServicioUsuario userService;
    private final ServicioPedidoCopisteria copisteriaOrderService;

    /**
     * Inyecta servicios de usuario y pedidos.
     */
    private final RepositorioUsuario userRepository;

    /**
     * Inyecta servicios de usuario/pedidos y repositorio.
     */
    private final ServicioTienda servicioTienda;

    public ControladorAreaPersonal(ServicioUsuario userService, ServicioPedidoCopisteria copisteriaOrderService, RepositorioUsuario userRepository, ServicioTienda servicioTienda) {
        this.userService = userService;
        this.copisteriaOrderService = copisteriaOrderService;
        this.userRepository = userRepository;
        this.servicioTienda = servicioTienda;
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
        return "redirect:/copisteria";
    }

    /**
     * Página admin con lista de todos los usuarios.
     */
    @GetMapping("/admin")
    public String admin(
            Authentication authentication, 
            Model model,
            @RequestParam(name = "updated", defaultValue = "false") boolean updated
    ) {
        Usuario currentUsuario = userService.findRequiredByEmail(authentication.getName());
        model.addAttribute("currentUsuario", currentUsuario);
        model.addAttribute("roleUpdated", updated);
        model.addAttribute("allUsers", userRepository.findAll());
        return "administracion/inicio";
    }

    /**
     * Actualiza rol de usuario desde admin panel.
     */
@PostMapping("/admin/update-role/{id}")
    public String updateUserRole(@PathVariable Long id, @RequestParam Rol newRole) {
        Optional<Usuario> optionalUser = userRepository.findById(id);
        if (optionalUser.isPresent()) {
            Usuario user = optionalUser.get();
            user.setRol(newRole);
            userRepository.save(user);
        }
        return "redirect:/admin?updated=true";
    }

@GetMapping("/admin/api/users")
    @ResponseBody
    public List<Usuario> getUsersApi() {
        return userRepository.findAll();
    }

    @GetMapping("/admin/api/products")
    @ResponseBody
    public List<Producto> getProductsApi() {
        return servicioTienda.obtenerTodosProductos();
    }

    private void populatePersonalAreaModel(Model model, Usuario currentUsuario, FormularioActualizarPerfil profileForm, boolean updated) {
        model.addAttribute("currentUsuario", currentUsuario);
        model.addAttribute("profileForm", profileForm);
        model.addAttribute("profileUpdated", updated);
        model.addAttribute("pageTitle", "Maxcopias | Area personal");
    }
}

