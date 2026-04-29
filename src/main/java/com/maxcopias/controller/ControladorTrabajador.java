package com.maxcopias.controller;

import com.maxcopias.dto.DetallePedidoVista;
import com.maxcopias.model.EstadoPedidoCopisteria;
import com.maxcopias.model.EstadoPedidoTienda;
import com.maxcopias.model.Oferta;
import com.maxcopias.model.PedidoCopisteria;
import com.maxcopias.model.PedidoTienda;
import com.maxcopias.model.TipoOferta;
import com.maxcopias.model.Usuario;
import com.maxcopias.service.ServicioOferta;
import com.maxcopias.service.ServicioPedidosOperativos;
import com.maxcopias.service.ServicioTienda;
import com.maxcopias.service.ServicioUsuario;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ControladorTrabajador {

    private final ServicioUsuario servicioUsuario;
    private final ServicioTienda servicioTienda;
    private final ServicioPedidosOperativos servicioPedidosOperativos;
    private final ServicioOferta servicioOferta;

    public ControladorTrabajador(
        ServicioUsuario servicioUsuario,
        ServicioTienda servicioTienda,
        ServicioPedidosOperativos servicioPedidosOperativos,
        ServicioOferta servicioOferta
    ) {
        this.servicioUsuario = servicioUsuario;
        this.servicioTienda = servicioTienda;
        this.servicioPedidosOperativos = servicioPedidosOperativos;
        this.servicioOferta = servicioOferta;
    }

    @GetMapping("/worker")
    public String panelTrabajador(Authentication authentication, Model model) {
        Usuario currentUsuario = servicioUsuario.findRequiredByEmail(authentication.getName());
        populateWorkerBase(model, currentUsuario, "resumen");
        model.addAttribute("contadores", servicioPedidosOperativos.obtenerContadoresTrabajo());
        model.addAttribute("pedidosCopisteria", servicioPedidosOperativos.obtenerPedidosCopisteria().stream().limit(4).toList());
        model.addAttribute("pedidosTienda", servicioPedidosOperativos.obtenerPedidosTienda().stream().limit(4).toList());
        model.addAttribute("pageTitle", "Maxcopias | Panel trabajador");
        return "trabajador/panel";
    }

    @GetMapping("/worker/productos")
    public String productos(Authentication authentication, Model model) {
        Usuario currentUsuario = servicioUsuario.findRequiredByEmail(authentication.getName());
        populateWorkerBase(model, currentUsuario, "productos");
        model.addAttribute("productos", servicioTienda.obtenerTodosProductos());
        model.addAttribute("categorias", servicioTienda.obtenerTodasCategorias());
        model.addAttribute("pageTitle", "Maxcopias | Productos trabajador");
        return "trabajador/productos";
    }

    @GetMapping("/worker/pedidos-copisteria")
    public String pedidosCopisteria(Authentication authentication, Model model) {
        Usuario currentUsuario = servicioUsuario.findRequiredByEmail(authentication.getName());
        populateWorkerBase(model, currentUsuario, "copisteria");
        model.addAttribute("pedidosCopisteria", servicioPedidosOperativos.obtenerPedidosCopisteria());
        model.addAttribute("estadosCopisteria", EstadoPedidoCopisteria.values());
        model.addAttribute("pageTitle", "Maxcopias | Pedidos copisteria");
        return "trabajador/pedidos-copisteria";
    }

    @GetMapping("/worker/pedidos-tienda")
    public String pedidosTienda(Authentication authentication, Model model) {
        Usuario currentUsuario = servicioUsuario.findRequiredByEmail(authentication.getName());
        populateWorkerBase(model, currentUsuario, "tienda");
        model.addAttribute("pedidosTienda", servicioPedidosOperativos.obtenerPedidosTienda());
        model.addAttribute("estadosTienda", EstadoPedidoTienda.values());
        model.addAttribute("pageTitle", "Maxcopias | Pedidos tienda");
        return "trabajador/pedidos-tienda";
    }

    @GetMapping("/worker/pedidos/{id}")
    public String detallePedidoWorker(
        @PathVariable Long id,
        @RequestParam String tipo,
        Authentication authentication,
        Model model
    ) {
        Usuario currentUsuario = servicioUsuario.findRequiredByEmail(authentication.getName());
        boolean esCopisteria = "copisteria".equalsIgnoreCase(tipo);

        populateWorkerBase(model, currentUsuario, esCopisteria ? "copisteria" : "tienda");

        if (esCopisteria) {
            PedidoCopisteria pedido = servicioPedidosOperativos.obtenerPedidoCopisteriaActivo(id);
            DetallePedidoVista detalle = servicioPedidosOperativos.construirDetallePedidoCopisteria(pedido);
            model.addAttribute("detallePedido", detalle);
            model.addAttribute("estadoOptions", EstadoPedidoCopisteria.values());
            model.addAttribute("estadoAction", "/worker/pedidos/" + id + "/estado");
            model.addAttribute("tipoPedido", "copisteria");
            model.addAttribute("timelineStates", EstadoPedidoCopisteria.values());
            model.addAttribute("currentStepIndex", Math.max(0, IntStream.range(0, EstadoPedidoCopisteria.values().length)
                .filter(index -> EstadoPedidoCopisteria.values()[index] == pedido.getEstado())
                .findFirst()
                .orElse(0)));
        } else {
            PedidoTienda pedido = servicioPedidosOperativos.obtenerPedidoTiendaActivo(id);
            DetallePedidoVista detalle = servicioPedidosOperativos.construirDetallePedidoTienda(pedido);
            model.addAttribute("detallePedido", detalle);
            model.addAttribute("estadoOptions", EstadoPedidoTienda.values());
            model.addAttribute("estadoAction", "/worker/pedidos/" + id + "/estado");
            model.addAttribute("tipoPedido", "tienda");
            model.addAttribute("timelineStates", EstadoPedidoTienda.values());
            model.addAttribute("currentStepIndex", Math.max(0, IntStream.range(0, EstadoPedidoTienda.values().length)
                .filter(index -> EstadoPedidoTienda.values()[index] == pedido.getEstado())
                .findFirst()
                .orElse(0)));
        }

        model.addAttribute("panelTipo", "worker");
        model.addAttribute("backUrl", esCopisteria ? "/worker/pedidos-copisteria" : "/worker/pedidos-tienda");
        model.addAttribute("backLabel", esCopisteria ? "Volver a pedidos de copisteria" : "Volver a pedidos de tienda");
        model.addAttribute("pageTitle", "Maxcopias | Detalle pedido");
        return "pedidos/detalle";
    }

    @GetMapping("/worker/ofertas")
    public String ofertas(Authentication authentication, Model model) {
        Usuario currentUsuario = servicioUsuario.findRequiredByEmail(authentication.getName());
        populateWorkerBase(model, currentUsuario, "ofertas");
        model.addAttribute("ofertas", servicioOferta.obtenerTodas());
        model.addAttribute("pageTitle", "Maxcopias | Ofertas trabajador");
        return "trabajador/ofertas";
    }

    @PostMapping("/worker/copisteria/{id}/estado")
    public String cambiarEstadoCopisteria(@PathVariable Long id, @RequestParam EstadoPedidoCopisteria estado) {
        servicioPedidosOperativos.cambiarEstadoCopisteria(id, estado);
        return "redirect:/worker/pedidos-copisteria";
    }

    @PostMapping("/worker/copisteria/{id}/eliminar")
    public String eliminarPedidoCopisteria(@PathVariable Long id, Authentication authentication) {
        servicioPedidosOperativos.eliminarPedidoCopisteria(id, authentication.getName());
        return "redirect:/worker/pedidos-copisteria";
    }

    @PostMapping("/worker/tienda/{id}/estado")
    public String cambiarEstadoTienda(@PathVariable Long id, @RequestParam EstadoPedidoTienda estado) {
        servicioPedidosOperativos.cambiarEstadoTienda(id, estado);
        return "redirect:/worker/pedidos-tienda";
    }

    @PostMapping("/worker/tienda/{id}/eliminar")
    public String eliminarPedidoTienda(@PathVariable Long id, Authentication authentication) {
        servicioPedidosOperativos.eliminarPedidoTienda(id, authentication.getName());
        return "redirect:/worker/pedidos-tienda";
    }

    @PostMapping("/worker/pedidos/{id}/estado")
    public String cambiarEstadoDesdeDetalleWorker(
        @PathVariable Long id,
        @RequestParam String tipo,
        @RequestParam String estado
    ) {
        if ("copisteria".equalsIgnoreCase(tipo)) {
            servicioPedidosOperativos.cambiarEstadoCopisteria(id, EstadoPedidoCopisteria.valueOf(estado));
            return "redirect:/worker/pedidos/" + id + "?tipo=copisteria";
        }

        servicioPedidosOperativos.cambiarEstadoTienda(id, EstadoPedidoTienda.valueOf(estado));
        return "redirect:/worker/pedidos/" + id + "?tipo=tienda";
    }

    @PostMapping("/worker/productos/{id}/activo")
    public String cambiarActivoProducto(@PathVariable Long id, @RequestParam boolean activo) {
        servicioTienda.cambiarActivoProducto(id, activo);
        return "redirect:/worker/productos";
    }

    @GetMapping("/worker/ofertas/nueva")
    public String nuevaOferta(Authentication authentication, Model model) {
        Usuario currentUsuario = servicioUsuario.findRequiredByEmail(authentication.getName());
        populateWorkerBase(model, currentUsuario, "ofertas");
        model.addAttribute("currentUsuario", currentUsuario);
        model.addAttribute("oferta", servicioOferta.nuevaOfertaBase());
        model.addAttribute("productos", servicioTienda.obtenerTodosProductos());
        model.addAttribute("categorias", servicioTienda.obtenerTodasCategorias());
        model.addAttribute("tiposOferta", TipoOferta.values());
        model.addAttribute("productoIdsSeleccionados", Set.of());
        model.addAttribute("pageTitle", "Maxcopias | Nueva oferta");
        model.addAttribute("formAction", "/worker/ofertas");
        return "trabajador/oferta-form";
    }

    @PostMapping("/worker/ofertas")
    public String guardarNuevaOferta(
        @ModelAttribute Oferta oferta,
        @RequestParam(value = "productoIds", required = false) List<Long> productoIds,
        @RequestParam(value = "categoriaId", required = false) Long categoriaId,
        Authentication authentication,
        Model model
    ) {
        try {
            guardarOfertaDesdeFormulario(oferta, productoIds, categoriaId);
            return "redirect:/worker/ofertas";
        } catch (IllegalArgumentException exception) {
            prepararModeloFormularioOferta(authentication, model, oferta, productoIds, "Maxcopias | Nueva oferta", "/worker/ofertas");
            model.addAttribute("error", exception.getMessage());
            return "trabajador/oferta-form";
        }
    }

    @GetMapping("/worker/ofertas/{id}/editar")
    public String editarOferta(@PathVariable Long id, Authentication authentication, Model model) {
        Usuario currentUsuario = servicioUsuario.findRequiredByEmail(authentication.getName());
        populateWorkerBase(model, currentUsuario, "ofertas");
        model.addAttribute("currentUsuario", currentUsuario);
        Oferta oferta = servicioOferta.obtenerObligatoria(id);
        model.addAttribute("oferta", oferta);
        model.addAttribute("productos", servicioTienda.obtenerTodosProductos());
        model.addAttribute("categorias", servicioTienda.obtenerTodasCategorias());
        model.addAttribute("tiposOferta", TipoOferta.values());
        model.addAttribute("productoIdsSeleccionados", obtenerProductoIdsSeleccionados(oferta));
        model.addAttribute("pageTitle", "Maxcopias | Editar oferta");
        model.addAttribute("formAction", "/worker/ofertas/" + id);
        return "trabajador/oferta-form";
    }

    @PostMapping("/worker/ofertas/{id}")
    public String actualizarOferta(
        @PathVariable Long id,
        @ModelAttribute Oferta oferta,
        @RequestParam(value = "productoIds", required = false) List<Long> productoIds,
        @RequestParam(value = "categoriaId", required = false) Long categoriaId,
        Authentication authentication,
        Model model
    ) {
        Oferta existente = servicioOferta.obtenerObligatoria(id);
        existente.setTitulo(oferta.getTitulo());
        existente.setDescripcion(oferta.getDescripcion());
        existente.setPrecioDescuento(oferta.getPrecioDescuento());
        existente.setImagenUrl(oferta.getImagenUrl());
        existente.setFechaInicio(oferta.getFechaInicio());
        existente.setFechaFin(oferta.getFechaFin());
        existente.setActiva(oferta.getActiva());
        existente.setPrincipal(oferta.getPrincipal());
        existente.setTipoOferta(oferta.getTipoOferta());
        existente.setPorcentajeDescuento(oferta.getPorcentajeDescuento());
        try {
            guardarOfertaDesdeFormulario(existente, productoIds, categoriaId);
            return "redirect:/worker/ofertas";
        } catch (IllegalArgumentException exception) {
            prepararModeloFormularioOferta(authentication, model, existente, productoIds, "Maxcopias | Editar oferta", "/worker/ofertas/" + id);
            model.addAttribute("error", exception.getMessage());
            return "trabajador/oferta-form";
        }
    }

    @PostMapping("/worker/ofertas/{id}/activa")
    public String cambiarOfertaActiva(@PathVariable Long id, @RequestParam boolean activa) {
        servicioOferta.cambiarActiva(id, activa);
        return "redirect:/worker/ofertas";
    }

    @PostMapping("/worker/ofertas/{id}/principal")
    public String marcarOfertaPrincipal(@PathVariable Long id) {
        servicioOferta.marcarPrincipal(id);
        return "redirect:/worker/ofertas";
    }

    private void populateWorkerBase(Model model, Usuario currentUsuario, String activeSection) {
        model.addAttribute("currentUsuario", currentUsuario);
        model.addAttribute("activeWorkerSection", activeSection);
        model.addAttribute("contadoresCopisteria", servicioPedidosOperativos.obtenerContadoresCopisteriaResumen());
        model.addAttribute("contadoresTienda", servicioPedidosOperativos.obtenerContadoresTiendaResumen());
    }

    private void guardarOfertaDesdeFormulario(Oferta oferta, List<Long> productoIds, Long categoriaId) {
        if (oferta.getTipoOferta() == TipoOferta.PRODUCTO && productoIds != null && !productoIds.isEmpty()) {
            oferta.setProducto(null);
            oferta.clearProductos();
            productoIds.stream()
                .distinct()
                .map(servicioTienda::obtenerProductoPorId)
                .forEach(oferta::addProducto);
            oferta.setCategoria(null);
        } else if (oferta.getTipoOferta() == TipoOferta.CATEGORIA && categoriaId != null) {
            oferta.setCategoria(servicioTienda.obtenerCategoriaObligatoria(categoriaId));
            oferta.setProducto(null);
            oferta.clearProductos();
        } else {
            oferta.setProducto(null);
            oferta.clearProductos();
            oferta.setCategoria(null);
        }

        servicioOferta.guardar(oferta);
    }

    private void prepararModeloFormularioOferta(
        Authentication authentication,
        Model model,
        Oferta oferta,
        List<Long> productoIds,
        String pageTitle,
        String formAction
    ) {
        Usuario currentUsuario = servicioUsuario.findRequiredByEmail(authentication.getName());
        populateWorkerBase(model, currentUsuario, "ofertas");
        model.addAttribute("currentUsuario", currentUsuario);
        model.addAttribute("oferta", oferta);
        model.addAttribute("productos", servicioTienda.obtenerTodosProductos());
        model.addAttribute("categorias", servicioTienda.obtenerTodasCategorias());
        model.addAttribute("tiposOferta", TipoOferta.values());
        model.addAttribute("productoIdsSeleccionados", productoIds == null ? Set.of() : new LinkedHashSet<>(productoIds));
        model.addAttribute("pageTitle", pageTitle);
        model.addAttribute("formAction", formAction);
    }

    private Set<Long> obtenerProductoIdsSeleccionados(Oferta oferta) {
        Set<Long> ids = new LinkedHashSet<>();
        if (oferta.getProducto() != null) {
            ids.add(oferta.getProducto().getId());
        }
        if (oferta.getProductos() != null) {
            oferta.getProductos().forEach(producto -> ids.add(producto.getId()));
        }
        return ids;
    }
}
