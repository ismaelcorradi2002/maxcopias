package com.maxcopias.service;

import com.maxcopias.dto.CarritoItemVista;
import com.maxcopias.dto.CarritoVista;
import com.maxcopias.model.Carrito;
import com.maxcopias.model.CarritoItem;
import com.maxcopias.model.MetodoEntregaPedidoTienda;
import com.maxcopias.model.Producto;
import com.maxcopias.model.Usuario;
import com.maxcopias.repository.RepositorioCarrito;
import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicioCarrito {

    private static final String SESSION_DELIVERY_KEY = "maxcopias.cart.deliveryMethod";
    private static final String SESSION_CART_ID_KEY = "maxcopias.cart.id";
    private static final Locale LOCALE_ES = new Locale("es", "ES");

    private final RepositorioCarrito repositorioCarrito;
    private final ServicioTienda servicioTienda;
    private final ServicioCatalogoTiendaVisual servicioCatalogoTiendaVisual;
    private final ServicioUsuario servicioUsuario;

    public ServicioCarrito(
        RepositorioCarrito repositorioCarrito,
        ServicioTienda servicioTienda,
        ServicioCatalogoTiendaVisual servicioCatalogoTiendaVisual,
        ServicioUsuario servicioUsuario
    ) {
        this.repositorioCarrito = repositorioCarrito;
        this.servicioTienda = servicioTienda;
        this.servicioCatalogoTiendaVisual = servicioCatalogoTiendaVisual;
        this.servicioUsuario = servicioUsuario;
    }

    @Transactional
    public void anadirProducto(Long productoId, int cantidad, Authentication authentication, HttpSession session) {
        Producto producto = servicioTienda.obtenerProductoObligatorio(productoId);
        validarProductoComprable(producto);
        int cantidadNormalizada = Math.max(1, cantidad);

        Carrito carrito = obtenerCarritoActivo(authentication, session);
        CarritoItem item = carrito.getItems().stream()
            .filter(existing -> existing.getProducto().getId().equals(productoId))
            .findFirst()
            .orElseGet(() -> {
                CarritoItem nuevoItem = new CarritoItem();
                nuevoItem.setProducto(producto);
                nuevoItem.setCantidad(0);
                carrito.addItem(nuevoItem);
                return nuevoItem;
            });

        int nuevaCantidad = item.getCantidad() + cantidadNormalizada;
        validarStock(producto, nuevaCantidad);
        item.setCantidad(nuevaCantidad);
        Carrito guardado = repositorioCarrito.save(carrito);
        registrarCarritoEnSesion(session, guardado);
    }

    @Transactional
    public void actualizarCantidad(Long productoId, int cantidad, Authentication authentication, HttpSession session) {
        Carrito carrito = obtenerCarritoActivo(authentication, session);
        CarritoItem item = carrito.getItems().stream()
            .filter(existing -> existing.getProducto().getId().equals(productoId))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("El producto no existe en el carrito."));

        if (cantidad <= 0) {
            carrito.removeItem(item);
            Carrito guardado = repositorioCarrito.save(carrito);
            registrarCarritoEnSesion(session, guardado);
            return;
        }

        validarStock(item.getProducto(), cantidad);
        item.setCantidad(cantidad);
        Carrito guardado = repositorioCarrito.save(carrito);
        registrarCarritoEnSesion(session, guardado);
    }

    @Transactional
    public void eliminarProducto(Long productoId, Authentication authentication, HttpSession session) {
        Carrito carrito = obtenerCarritoActivo(authentication, session);
        carrito.getItems().stream()
            .filter(existing -> existing.getProducto().getId().equals(productoId))
            .findFirst()
            .ifPresent(carrito::removeItem);
        Carrito guardado = repositorioCarrito.save(carrito);
        registrarCarritoEnSesion(session, guardado);
    }

    @Transactional
    public void vaciar(Authentication authentication, HttpSession session) {
        Carrito carrito = obtenerCarritoActivo(authentication, session);
        carrito.clearItems();
        Carrito guardado = repositorioCarrito.save(carrito);
        registrarCarritoEnSesion(session, guardado);
    }

    @Transactional(readOnly = true)
    public CarritoVista obtenerVista(Authentication authentication, HttpSession session) {
        return construirVista(obtenerCarritoSoloLectura(authentication, session), obtenerMetodoEntregaSeleccionado(session), !isAuthenticated(authentication));
    }

    @Transactional(readOnly = true)
    public CarritoVista obtenerVista(Authentication authentication, HttpSession session, MetodoEntregaPedidoTienda metodoEntrega) {
        return construirVista(obtenerCarritoSoloLectura(authentication, session), metodoEntrega, !isAuthenticated(authentication));
    }

    @Transactional(readOnly = true)
    public int contarItems(Authentication authentication, HttpSession session) {
        return obtenerCarritoSoloLectura(authentication, session).getItems().stream()
            .mapToInt(CarritoItem::getCantidad)
            .sum();
    }

    @Transactional
    public void actualizarMetodoEntregaSeleccionado(HttpSession session, MetodoEntregaPedidoTienda metodoEntrega) {
        session.setAttribute(SESSION_DELIVERY_KEY, metodoEntrega == null ? MetodoEntregaPedidoTienda.ENVIO_DOMICILIO.name() : metodoEntrega.name());
    }

    public MetodoEntregaPedidoTienda obtenerMetodoEntregaSeleccionado(HttpSession session) {
        Object rawValue = session.getAttribute(SESSION_DELIVERY_KEY);
        if (rawValue instanceof String value) {
            try {
                return MetodoEntregaPedidoTienda.valueOf(value);
            } catch (IllegalArgumentException ignored) {
                return MetodoEntregaPedidoTienda.ENVIO_DOMICILIO;
            }
        }
        return MetodoEntregaPedidoTienda.ENVIO_DOMICILIO;
    }

    @Transactional
    public Carrito obtenerCarritoActivo(Authentication authentication, HttpSession session) {
        String sessionId = session.getId();
        Long carritoSesionId = obtenerCarritoIdSesion(session);

        if (isAuthenticated(authentication)) {
            Usuario usuario = servicioUsuario.findRequiredByEmail(authentication.getName());
            Optional<Carrito> carritoUsuario = repositorioCarrito.findByUsuarioIdAndActivoTrue(usuario.getId());
            Optional<Carrito> carritoSesion = buscarCarritoSesion(carritoSesionId, sessionId);

            Carrito destino = carritoUsuario.orElseGet(() -> crearCarritoUsuario(usuario, sessionId));
            if (carritoSesion.isPresent() && !carritoSesion.get().getId().equals(destino.getId())) {
                fusionarCarritos(carritoSesion.get(), destino);
            }
            destino.setSessionId(sessionId);
            Carrito guardado = repositorioCarrito.save(destino);
            registrarCarritoEnSesion(session, guardado);
            return guardado;
        }

        Optional<Carrito> carritoSesion = buscarCarritoSesion(carritoSesionId, sessionId);
        if (carritoSesion.isPresent()) {
            Carrito carrito = carritoSesion.get();
            if (carrito.getSessionId() == null || !sessionId.equals(carrito.getSessionId())) {
                carrito.setSessionId(sessionId);
                carrito = repositorioCarrito.save(carrito);
            }
            registrarCarritoEnSesion(session, carrito);
            return carrito;
        }

        Carrito guardado = repositorioCarrito.save(crearCarritoSesion(sessionId));
        registrarCarritoEnSesion(session, guardado);
        return guardado;
    }

    @Transactional(readOnly = true)
    public Carrito obtenerCarritoSoloLectura(Authentication authentication, HttpSession session) {
        String sessionId = session.getId();
        Long carritoSesionId = obtenerCarritoIdSesion(session);

        if (isAuthenticated(authentication)) {
            Usuario usuario = servicioUsuario.findRequiredByEmail(authentication.getName());
            Optional<Carrito> carritoUsuario = repositorioCarrito.findByUsuarioIdAndActivoTrue(usuario.getId());
            if (carritoUsuario.isPresent()) {
                registrarCarritoEnSesion(session, carritoUsuario.get());
                return carritoUsuario.get();
            }

            return buscarCarritoSesion(carritoSesionId, sessionId)
                .orElseGet(this::crearCarritoVacio);
        }

        return buscarCarritoSesion(carritoSesionId, sessionId)
            .orElseGet(this::crearCarritoVacio);
    }

    @Transactional
    public void limpiarCarrito(Carrito carrito) {
        carrito.clearItems();
        repositorioCarrito.save(carrito);
    }

    @Transactional
    public void sincronizarTrasLogin(Authentication authentication, HttpSession session) {
        if (!isAuthenticated(authentication) || session == null) {
            return;
        }
        Carrito carrito = obtenerCarritoActivo(authentication, session);
        registrarCarritoEnSesion(session, carrito);
    }

    private Optional<Carrito> buscarCarritoSesion(Long carritoSesionId, String sessionId) {
        if (carritoSesionId != null) {
            Optional<Carrito> carritoPorId = repositorioCarrito.findById(carritoSesionId)
                .filter(Carrito::isActivo);
            if (carritoPorId.isPresent()) {
                return carritoPorId;
            }
        }
        return repositorioCarrito.findBySessionIdAndActivoTrue(sessionId);
    }

    private Carrito crearCarritoUsuario(Usuario usuario, String sessionId) {
        Carrito carrito = new Carrito();
        carrito.setUsuario(usuario);
        carrito.setSessionId(sessionId);
        carrito.setActivo(true);
        return carrito;
    }

    private Carrito crearCarritoSesion(String sessionId) {
        Carrito carrito = new Carrito();
        carrito.setSessionId(sessionId);
        carrito.setActivo(true);
        return carrito;
    }

    private void fusionarCarritos(Carrito origen, Carrito destino) {
        for (CarritoItem itemOrigen : List.copyOf(origen.getItems())) {
            CarritoItem itemDestino = destino.getItems().stream()
                .filter(existing -> existing.getProducto().getId().equals(itemOrigen.getProducto().getId()))
                .findFirst()
                .orElse(null);

            if (itemDestino == null) {
                CarritoItem nuevoItem = new CarritoItem();
                nuevoItem.setProducto(itemOrigen.getProducto());
                nuevoItem.setCantidad(itemOrigen.getCantidad());
                destino.addItem(nuevoItem);
            } else {
                int cantidadFusionada = itemDestino.getCantidad() + itemOrigen.getCantidad();
                validarStock(itemDestino.getProducto(), cantidadFusionada);
                itemDestino.setCantidad(cantidadFusionada);
            }
        }

        origen.clearItems();
        origen.setActivo(false);
        repositorioCarrito.save(origen);
    }

    private Carrito crearCarritoVacio() {
        Carrito carrito = new Carrito();
        carrito.setActivo(true);
        return carrito;
    }

    private void registrarCarritoEnSesion(HttpSession session, Carrito carrito) {
        if (session == null || carrito == null || carrito.getId() == null) {
            return;
        }
        session.setAttribute(SESSION_CART_ID_KEY, carrito.getId());
    }

    private Long obtenerCarritoIdSesion(HttpSession session) {
        if (session == null) {
            return null;
        }
        Object rawValue = session.getAttribute(SESSION_CART_ID_KEY);
        if (rawValue instanceof Long longValue) {
            return longValue;
        }
        if (rawValue instanceof Integer intValue) {
            return intValue.longValue();
        }
        if (rawValue instanceof String stringValue) {
            try {
                return Long.parseLong(stringValue);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private CarritoVista construirVista(Carrito carrito, MetodoEntregaPedidoTienda metodoEntrega, boolean requiereLoginParaConfirmar) {
        List<CarritoItemVista> items = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        int totalItems = 0;

        for (CarritoItem item : carrito.getItems()) {
            Producto producto = item.getProducto();
            BigDecimal precioUnitario = money(producto.getPrecio());
            BigDecimal subtotalItem = money(precioUnitario.multiply(BigDecimal.valueOf(item.getCantidad())));
            items.add(new CarritoItemVista(
                producto.getId(),
                producto.getNombre(),
                producto.getDescripcion(),
                servicioCatalogoTiendaVisual.mapearProducto(producto).imagenUrl(),
                precioUnitario,
                formatearDinero(precioUnitario),
                item.getCantidad(),
                producto.getStock() == null ? 0 : producto.getStock(),
                subtotalItem,
                formatearDinero(subtotalItem)
            ));
            subtotal = subtotal.add(subtotalItem);
            totalItems += item.getCantidad();
        }

        BigDecimal gastosEnvio = metodoEntrega == MetodoEntregaPedidoTienda.ENVIO_DOMICILIO ? new BigDecimal("4.99") : BigDecimal.ZERO;
        BigDecimal total = subtotal.add(gastosEnvio);

        return new CarritoVista(
            List.copyOf(items),
            items.isEmpty(),
            totalItems,
            subtotal,
            formatearDinero(subtotal),
            gastosEnvio,
            formatearDinero(gastosEnvio),
            total,
            formatearDinero(total),
            metodoEntrega,
            requiereLoginParaConfirmar
        );
    }

    private void validarProductoComprable(Producto producto) {
        if (!producto.isActivo()) {
            throw new IllegalArgumentException("El producto no esta disponible en este momento.");
        }
        if (producto.getStock() == null || producto.getStock() <= 0) {
            throw new IllegalArgumentException("No hay stock disponible para este producto.");
        }
    }

    private void validarStock(Producto producto, int cantidad) {
        validarProductoComprable(producto);
        if (cantidad > producto.getStock()) {
            throw new IllegalArgumentException("La cantidad solicitada supera el stock disponible.");
        }
    }

    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal());
    }

    private BigDecimal money(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor.setScale(2, RoundingMode.HALF_UP);
    }

    private String formatearDinero(BigDecimal valor) {
        return NumberFormat.getCurrencyInstance(LOCALE_ES).format(money(valor));
    }
}
