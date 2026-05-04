package com.maxcopias.service;

import com.maxcopias.dto.CarritoItemVista;
import com.maxcopias.dto.CarritoVista;
import com.maxcopias.dto.FormularioCheckoutTienda;
import com.maxcopias.dto.ResumenPedidoTiendaVista;
import com.maxcopias.model.Carrito;
import com.maxcopias.model.CarritoItem;
import com.maxcopias.model.EstadoPedidoTienda;
import com.maxcopias.model.MetodoEntregaPedidoTienda;
import com.maxcopias.model.MetodoPagoPedidoTienda;
import com.maxcopias.model.PedidoItem;
import com.maxcopias.model.PedidoTienda;
import com.maxcopias.model.Producto;
import com.maxcopias.model.Usuario;
import com.maxcopias.repository.RepositorioPedidoTienda;
import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ServicioPedidosTienda {

    private static final Locale LOCALE_ES = new Locale("es", "ES");

    private final RepositorioPedidoTienda repositorioPedidoTienda;
    private final ServicioCarrito servicioCarrito;
    private final ServicioCatalogoTiendaVisual servicioCatalogoTiendaVisual;
    private final ServicioUsuario servicioUsuario;
    private final ServicioTienda servicioTienda;

    public ServicioPedidosTienda(
        RepositorioPedidoTienda repositorioPedidoTienda,
        ServicioCarrito servicioCarrito,
        ServicioCatalogoTiendaVisual servicioCatalogoTiendaVisual,
        ServicioUsuario servicioUsuario,
        ServicioTienda servicioTienda
    ) {
        this.repositorioPedidoTienda = repositorioPedidoTienda;
        this.servicioCarrito = servicioCarrito;
        this.servicioCatalogoTiendaVisual = servicioCatalogoTiendaVisual;
        this.servicioUsuario = servicioUsuario;
        this.servicioTienda = servicioTienda;
    }

    @Transactional
    public PedidoTienda confirmarPedido(FormularioCheckoutTienda formulario, Authentication authentication, HttpSession session) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new IllegalArgumentException("Debes iniciar sesion para confirmar el pedido.");
        }

        Usuario usuario = servicioUsuario.findRequiredByEmail(authentication.getName());
        Carrito carrito = servicioCarrito.obtenerCarritoActivo(authentication, session);
        if (carrito.getItems().isEmpty()) {
            throw new IllegalArgumentException("El carrito esta vacio.");
        }

        MetodoEntregaPedidoTienda metodoEntrega = formulario.getMetodoEntrega() == null
            ? MetodoEntregaPedidoTienda.RECOGIDA_TIENDA
            : formulario.getMetodoEntrega();
        CarritoVista carritoVista = servicioCarrito.obtenerVista(authentication, session, metodoEntrega);

        PedidoTienda pedido = new PedidoTienda();
        pedido.setUsuario(usuario);
        pedido.setCodigoPedido(generarCodigoPedidoUnico());
        pedido.setClienteNombre(normalizar(formulario.getNombre()));
        pedido.setEmail(normalizar(formulario.getEmail()).toLowerCase(Locale.ROOT));
        pedido.setTelefono(normalizar(formulario.getTelefono()));
        pedido.setMetodoEntrega(metodoEntrega);
        pedido.setEstado(EstadoPedidoTienda.PENDIENTE);
        pedido.setMetodoPago(metodoEntrega == MetodoEntregaPedidoTienda.RECOGIDA_TIENDA
            ? MetodoPagoPedidoTienda.PAGO_EN_TIENDA
            : MetodoPagoPedidoTienda.ENVIO_SIMULADO);
        pedido.setPagado(false);
        pedido.setSubtotal(carritoVista.subtotal());
        pedido.setGastosEnvio(carritoVista.gastosEnvio());
        pedido.setTotal(carritoVista.total());

        List<String> resumen = new ArrayList<>();
        for (CarritoItem itemCarrito : carrito.getItems()) {
            Producto productoActualizado = servicioTienda.obtenerProductoObligatorio(itemCarrito.getProducto().getId());
            validarStock(productoActualizado, itemCarrito.getCantidad());
            productoActualizado.setStock(productoActualizado.getStock() - itemCarrito.getCantidad());

            PedidoItem item = new PedidoItem();
            item.setPedido(pedido);
            item.setProducto(productoActualizado);
            item.setProductoNombre(productoActualizado.getNombre());
            item.setProductoImagenUrl(servicioCatalogoTiendaVisual.mapearProducto(productoActualizado).imagenUrl());
            item.setCantidad(itemCarrito.getCantidad());
            item.setPrecioUnitario(money(productoActualizado.getPrecio()));
            item.setSubtotal(money(productoActualizado.getPrecio().multiply(BigDecimal.valueOf(itemCarrito.getCantidad()))));
            pedido.getItems().add(item);
            resumen.add(productoActualizado.getNombre() + " x" + itemCarrito.getCantidad());
        }

        pedido.setResumenProductos(String.join(", ", resumen));
        PedidoTienda guardado = repositorioPedidoTienda.save(pedido);
        servicioCarrito.limpiarCarrito(carrito);
        servicioCarrito.actualizarMetodoEntregaSeleccionado(session, MetodoEntregaPedidoTienda.RECOGIDA_TIENDA);
        return guardado;
    }

    @Transactional(readOnly = true)
    public List<PedidoTienda> obtenerPedidosUsuario(Authentication authentication) {
        Usuario usuario = servicioUsuario.findRequiredByEmail(authentication.getName());
        return repositorioPedidoTienda.findAllByUsuarioAndEliminadoFalseOrderByFechaCreacionDesc(usuario);
    }

    @Transactional(readOnly = true)
    public ResumenPedidoTiendaVista construirResumen(PedidoTienda pedido) {
        List<CarritoItemVista> items = pedido.getItems().stream()
            .map(item -> new CarritoItemVista(
                item.getProducto() != null ? item.getProducto().getId() : null,
                item.getProductoNombre(),
                item.getProducto() != null ? item.getProducto().getDescripcion() : "",
                item.getProductoImagenUrl(),
                money(item.getPrecioUnitario()),
                formatearDinero(item.getPrecioUnitario()),
                item.getCantidad(),
                item.getProducto() != null && item.getProducto().getStock() != null ? item.getProducto().getStock() : 0,
                money(item.getSubtotal()),
                formatearDinero(item.getSubtotal())
            ))
            .toList();

        return new ResumenPedidoTiendaVista(
            pedido.getId(),
            pedido.getCodigoPedido(),
            pedido.getFechaCreacion(),
            items,
            money(pedido.getSubtotal()),
            formatearDinero(pedido.getSubtotal()),
            money(pedido.getGastosEnvio()),
            formatearDinero(pedido.getGastosEnvio()),
            money(pedido.getTotal()),
            formatearDinero(pedido.getTotal()),
            pedido.getMetodoEntrega(),
            pedido.getMetodoEntrega() != null ? pedido.getMetodoEntrega().getLabel() : "",
            pedido.getEstado(),
            pedido.getEstado() != null ? pedido.getEstado().getLabel() : "",
            resolverMensajeEstado(pedido),
            pedido.isPagado()
        );
    }

    @Transactional(readOnly = true)
    public PedidoTienda obtenerPedidoUsuario(Long pedidoId, Authentication authentication) {
        Usuario usuario = servicioUsuario.findRequiredByEmail(authentication.getName());
        return repositorioPedidoTienda.findById(pedidoId)
            .filter(pedido -> pedido.getUsuario() != null && pedido.getUsuario().getId().equals(usuario.getId()))
            .orElseThrow(() -> new IllegalArgumentException("No existe el pedido indicado."));
    }

    public String resolverMensajeEstado(PedidoTienda pedido) {
        if (pedido.getEstado() == null) {
            return "Pedido recibido";
        }

        return switch (pedido.getEstado()) {
            case PENDIENTE -> "Pedido recibido";
            case EN_PREPARACION -> "Pedido en preparacion";
            case LISTO_PARA_RECOGER -> pedido.getMetodoEntrega() == MetodoEntregaPedidoTienda.RECOGIDA_TIENDA
                ? "Tu pedido esta listo para recoger en tienda"
                : "Pedido preparado";
            case ENTREGADO -> "Pedido entregado";
            case CANCELADO -> "Pedido cancelado";
        };
    }

    public String resolverMensajeConfirmacion(PedidoTienda pedido) {
        if (pedido.getMetodoEntrega() == MetodoEntregaPedidoTienda.ENVIO_DOMICILIO) {
            return "Pedido recibido. El envio es una simulacion de momento.";
        }
        return "Pedido recibido. Lo prepararemos y te avisaremos cuando este listo para recoger.";
    }

    @Transactional
    public void cambiarEstado(Long pedidoId, EstadoPedidoTienda nuevoEstado) {
        PedidoTienda pedido = repositorioPedidoTienda.findByIdAndEliminadoFalse(pedidoId)
            .orElseThrow(() -> new IllegalArgumentException("No existe el pedido de tienda indicado."));
        EstadoPedidoTienda estadoAnterior = pedido.getEstado();

        if (pedido.getMetodoEntrega() == MetodoEntregaPedidoTienda.ENVIO_DOMICILIO
            && nuevoEstado == EstadoPedidoTienda.LISTO_PARA_RECOGER) {
            throw new IllegalArgumentException("El envio a domicilio no puede pasar a listo para recoger.");
        }

        pedido.setEstado(nuevoEstado);
        if (nuevoEstado == EstadoPedidoTienda.CANCELADO && estadoAnterior != EstadoPedidoTienda.CANCELADO) {
            reponerStock(pedido);
        }
        repositorioPedidoTienda.save(pedido);
    }

    @Transactional
    public void marcarPagado(Long pedidoId, boolean pagado) {
        PedidoTienda pedido = repositorioPedidoTienda.findByIdAndEliminadoFalse(pedidoId)
            .orElseThrow(() -> new IllegalArgumentException("No existe el pedido de tienda indicado."));
        pedido.setPagado(pagado);
        pedido.setFechaPago(pagado ? LocalDateTime.now() : null);
        repositorioPedidoTienda.save(pedido);
    }

    private void reponerStock(PedidoTienda pedido) {
        for (PedidoItem item : pedido.getItems()) {
            if (item.getProducto() == null || item.getCantidad() == null) {
                continue;
            }
            Producto producto = servicioTienda.obtenerProductoObligatorio(item.getProducto().getId());
            int stockActual = producto.getStock() == null ? 0 : producto.getStock();
            producto.setStock(stockActual + item.getCantidad());
        }
    }

    private void validarStock(Producto producto, int cantidad) {
        if (!producto.isActivo()) {
            throw new IllegalArgumentException("Hay productos del carrito que ya no estan disponibles.");
        }
        if (producto.getStock() == null || producto.getStock() < cantidad) {
            throw new IllegalArgumentException("Stock insuficiente para completar el pedido.");
        }
    }

    private String generarCodigoPedidoUnico() {
        String codigo;
        do {
            codigo = "MT-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase(Locale.ROOT);
        } while (repositorioPedidoTienda.existsByCodigoPedido(codigo));
        return codigo;
    }

    private String normalizar(String valor) {
        return StringUtils.hasText(valor) ? valor.trim() : "";
    }

    private BigDecimal money(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor.setScale(2, RoundingMode.HALF_UP);
    }

    private String formatearDinero(BigDecimal valor) {
        return NumberFormat.getCurrencyInstance(LOCALE_ES).format(money(valor));
    }
}
