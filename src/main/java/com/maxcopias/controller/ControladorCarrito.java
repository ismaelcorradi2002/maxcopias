package com.maxcopias.controller;

import com.maxcopias.dto.ConfirmacionPedidoTiendaVista;
import com.maxcopias.dto.FormularioCheckoutTienda;
import com.maxcopias.model.PedidoTienda;
import com.maxcopias.model.Usuario;
import com.maxcopias.service.ServicioCarrito;
import com.maxcopias.service.ServicioPedidosTienda;
import com.maxcopias.service.ServicioUsuario;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping
public class ControladorCarrito {

    private final ServicioCarrito servicioCarrito;
    private final ServicioPedidosTienda servicioPedidosTienda;
    private final ServicioUsuario servicioUsuario;

    public ControladorCarrito(
        ServicioCarrito servicioCarrito,
        ServicioPedidosTienda servicioPedidosTienda,
        ServicioUsuario servicioUsuario
    ) {
        this.servicioCarrito = servicioCarrito;
        this.servicioPedidosTienda = servicioPedidosTienda;
        this.servicioUsuario = servicioUsuario;
    }

    @GetMapping("/carrito")
    public String verCarrito(Authentication authentication, HttpSession session, Model model) {
        model.addAttribute("carrito", servicioCarrito.obtenerVista(authentication, session));
        model.addAttribute("pageTitle", "Maxcopias | Carrito");
        return "tienda/carrito";
    }

    @PostMapping("/carrito/anadir")
    public String anadirProducto(
        @RequestParam Long productoId,
        @RequestParam(defaultValue = "1") int cantidad,
        @RequestParam(required = false) String redirectTo,
        Authentication authentication,
        HttpSession session,
        RedirectAttributes redirectAttributes
    ) {
        try {
            servicioCarrito.anadirProducto(productoId, cantidad, authentication, session);
            redirectAttributes.addFlashAttribute("cartFeedback", "Producto anadido al carrito.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("cartError", exception.getMessage());
        }
        return "redirect:" + ((redirectTo != null && redirectTo.startsWith("/")) ? redirectTo : "/tienda");
    }

    @PostMapping("/carrito/actualizar")
    public String actualizarCantidad(
        @RequestParam Long productoId,
        @RequestParam int cantidad,
        Authentication authentication,
        HttpSession session,
        RedirectAttributes redirectAttributes
    ) {
        try {
            servicioCarrito.actualizarCantidad(productoId, cantidad, authentication, session);
            redirectAttributes.addFlashAttribute("cartFeedback", "Carrito actualizado.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("cartError", exception.getMessage());
        }
        return "redirect:/carrito";
    }

    @PostMapping("/carrito/eliminar")
    public String eliminarProducto(
        @RequestParam Long productoId,
        Authentication authentication,
        HttpSession session,
        RedirectAttributes redirectAttributes
    ) {
        servicioCarrito.eliminarProducto(productoId, authentication, session);
        redirectAttributes.addFlashAttribute("cartFeedback", "Producto eliminado del carrito.");
        return "redirect:/carrito";
    }

    @PostMapping("/carrito/entrega")
    public String actualizarEntrega(
        @RequestParam com.maxcopias.model.MetodoEntregaPedidoTienda metodoEntrega,
        HttpSession session
    ) {
        servicioCarrito.actualizarMetodoEntregaSeleccionado(session, metodoEntrega);
        return "redirect:/carrito";
    }

    @GetMapping("/checkout")
    public String checkout(Authentication authentication, HttpSession session, Model model) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return "redirect:/login";
        }
        if (servicioCarrito.obtenerVista(authentication, session).vacio()) {
            return "redirect:/carrito";
        }

        FormularioCheckoutTienda formulario = new FormularioCheckoutTienda();
        Usuario usuario = servicioUsuario.findRequiredByEmail(authentication.getName());
        return cargarCheckout(model, authentication, session, formulario, usuario);
    }

    @PostMapping("/checkout")
    public String confirmarCheckout(
        @Valid @ModelAttribute("checkoutForm") FormularioCheckoutTienda formulario,
        BindingResult bindingResult,
        Authentication authentication,
        HttpSession session,
        Model model,
        RedirectAttributes redirectAttributes
    ) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return "redirect:/login";
        }
        if (servicioCarrito.obtenerVista(authentication, session).vacio()) {
            return "redirect:/carrito";
        }

        servicioCarrito.actualizarMetodoEntregaSeleccionado(session, formulario.getMetodoEntrega());
        if (bindingResult.hasErrors()) {
            return cargarCheckout(model, authentication, session, formulario, null);
        }

        try {
            PedidoTienda pedido = servicioPedidosTienda.confirmarPedido(formulario, authentication, session);
            return "redirect:/pedido-confirmado/" + pedido.getId();
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("cartError", exception.getMessage());
            return "redirect:/carrito";
        }
    }

    @GetMapping("/pedido-confirmado/{id}")
    public String pedidoConfirmado(@PathVariable Long id, Authentication authentication, Model model) {
        PedidoTienda pedido = servicioPedidosTienda.obtenerPedidoUsuario(id, authentication);
        ConfirmacionPedidoTiendaVista confirmacion = new ConfirmacionPedidoTiendaVista(
            servicioPedidosTienda.construirResumen(pedido),
            servicioPedidosTienda.resolverMensajeConfirmacion(pedido)
        );
        model.addAttribute("confirmacion", confirmacion);
        model.addAttribute("pageTitle", "Maxcopias | Pedido confirmado");
        return "tienda/confirmacion-pedido";
    }

    private String cargarCheckout(
        Model model,
        Authentication authentication,
        HttpSession session,
        FormularioCheckoutTienda formulario,
        Usuario usuario
    ) {
        if (usuario != null && (formulario.getNombre() == null || formulario.getNombre().isBlank())) {
            formulario.setNombre(usuario.getFullName());
        }
        if (usuario != null && (formulario.getEmail() == null || formulario.getEmail().isBlank())) {
            formulario.setEmail(usuario.getEmail());
        }
        if (usuario != null && (formulario.getTelefono() == null || formulario.getTelefono().isBlank())) {
            formulario.setTelefono(usuario.getPhone());
        }
        if (formulario.getMetodoEntrega() == null) {
            formulario.setMetodoEntrega(servicioCarrito.obtenerMetodoEntregaSeleccionado(session));
        }
        model.addAttribute("checkoutForm", formulario);
        model.addAttribute("carrito", servicioCarrito.obtenerVista(authentication, session, formulario.getMetodoEntrega()));
        model.addAttribute("pageTitle", "Maxcopias | Checkout");
        return "tienda/checkout";
    }
}
