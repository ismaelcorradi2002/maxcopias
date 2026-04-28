package com.maxcopias.controller;

/**
 * Controlador del área personal del usuario (perfil, pedidos).
 */
import com.maxcopias.dto.FormularioActualizarPerfil;
import com.maxcopias.dto.PedidoAdminVista;
import com.maxcopias.model.Usuario;
import com.maxcopias.service.ServicioUsuario;
import com.maxcopias.model.Rol;
import com.maxcopias.repository.RepositorioCategoria;
import com.maxcopias.repository.RepositorioPedidoCopisteria;
import com.maxcopias.repository.RepositorioUsuario;
import com.maxcopias.service.ServicioPedidosOperativos;
import com.maxcopias.model.PedidoCopisteria;
import com.maxcopias.model.PedidoTienda;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.time.YearMonth;

import com.maxcopias.model.Producto;
import com.maxcopias.model.Categoria;
import java.math.BigDecimal;
import com.maxcopias.service.ServicioTienda;
import org.springframework.web.bind.annotation.ResponseBody;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;

@Controller
public class ControladorAreaPersonal {

    private final ServicioUsuario userService;

    /**
     * Inyecta servicios de usuario y pedidos.
     */
    private final RepositorioUsuario userRepository;
    private final RepositorioCategoria repositorioCategoria;
    private final RepositorioPedidoCopisteria repositorioPedidoCopisteria;

    /**
     * Inyecta servicios de usuario/pedidos y repositorio.
     */
    private final ServicioTienda servicioTienda;
    private final ServicioPedidosOperativos servicioPedidosOperativos;

    public ControladorAreaPersonal(ServicioUsuario userService, RepositorioUsuario userRepository, ServicioTienda servicioTienda, RepositorioCategoria repositorioCategoria, ServicioPedidosOperativos servicioPedidosOperativos, RepositorioPedidoCopisteria repositorioPedidoCopisteria) {
        this.repositorioCategoria = repositorioCategoria;
        this.userService = userService;
        this.userRepository = userRepository;
        this.servicioTienda = servicioTienda;
        this.servicioPedidosOperativos = servicioPedidosOperativos;
        this.repositorioPedidoCopisteria = repositorioPedidoCopisteria;
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
        model.addAttribute("orders", repositorioPedidoCopisteria.findAllByUsuarioAndEliminadoFalseOrderByFechaCreacionDesc(currentUsuario));
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
            @RequestParam(name = "updated", defaultValue = "false") boolean updated,
            @RequestParam(name = "roleProtected", defaultValue = "false") boolean roleProtected,
            @RequestParam(name = "tab", required = false) String tab
    ) {
        Usuario currentUsuario = userService.findRequiredByEmail(authentication.getName());
        model.addAttribute("currentUsuario", currentUsuario);
        model.addAttribute("roleUpdated", updated);
        model.addAttribute("roleProtected", roleProtected);
        model.addAttribute("allUsers", userRepository.findAll());
        model.addAttribute("contadoresCopisteria", servicioPedidosOperativos.obtenerContadoresCopisteriaResumen());
        model.addAttribute("contadoresTienda", servicioPedidosOperativos.obtenerContadoresTiendaResumen());
        model.addAttribute("activeTab", tab != null ? tab : "users");
        return "administracion/inicio";
    }

    @GetMapping("/admin/finanzas")
    public String finanzasAdmin(
        Authentication authentication,
        Model model,
        @RequestParam(name = "mes", required = false) Integer mes,
        @RequestParam(name = "anio", required = false) Integer anio
    ) {
        Usuario currentUsuario = userService.findRequiredByEmail(authentication.getName());
        YearMonth actual = YearMonth.now();
        int mesSeleccionado = mes != null ? mes : actual.getMonthValue();
        int anioSeleccionado = anio != null ? anio : actual.getYear();

        model.addAttribute("currentUsuario", currentUsuario);
        model.addAttribute("mesSeleccionado", mesSeleccionado);
        model.addAttribute("anioSeleccionado", anioSeleccionado);
        model.addAttribute("resumenFinanciero", servicioPedidosOperativos.obtenerResumenFinancieroMensual(anioSeleccionado, mesSeleccionado));
        model.addAttribute("pageTitle", "Maxcopias | Finanzas admin");
        return "administracion/finanzas";
    }

    /**
     * Actualiza rol de usuario desde admin panel.
     */
    @PostMapping("/admin/update-role/{id}")
    public String updateUserRole(@PathVariable Long id, @RequestParam Rol newRole) {
        Optional<Usuario> optionalUser = userRepository.findById(id);
        if (optionalUser.isPresent()) {
            Usuario user = optionalUser.get();
            if (user.getRol() == Rol.ROLE_ADMIN && newRole != Rol.ROLE_ADMIN && userRepository.countByRole(Rol.ROLE_ADMIN) <= 1) {
                return "redirect:/admin?roleProtected=true";
            }
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

    @GetMapping("/admin/api/categorias")
    @ResponseBody
    public List<Categoria> getCategoriasApi() {
        return servicioTienda.obtenerTodasCategorias();
    }

    @GetMapping("/admin/api/pedidos")
    @ResponseBody
    public List<PedidoAdminVista> getPedidosApi() {
        List<PedidoAdminVista> pedidos = new ArrayList<>();

        for (PedidoCopisteria p : servicioPedidosOperativos.obtenerPedidosCopisteriaIncluyendoEliminados()) {
            pedidos.add(new PedidoAdminVista(
                p.getId(),
                "Copistería",
                p.getCustomerName(),
                p.getEmail(),
                p.getPhone(),
                p.getEstado() != null ? p.getEstado().name() : null,
                p.getFechaCreacion(),
                p.getPrecio(),
                p.getTrabajo() != null ? p.getTrabajo().name() : null,
                p.getCopias(),
                p.getColor() != null ? p.getColor().name() : null,
                p.getTamano() != null ? p.getTamano().name() : null,
                p.getCaras() != null ? p.getCaras().name() : null,
                p.getPapel() != null ? p.getPapel().name() : null,
                p.getEncuadernacion() != null ? p.getEncuadernacion().name() : null,
                p.getExtras(),
                p.getNombreArchivo(),
                p.getRutaArchivo(),
                p.getRutaArchivo() != null ? "/pedidos/copisteria/" + p.getId() + "/archivo" : null,
                p.getCodigoRecoger(),
                null,
                p.getUsuario() != null ? p.getUsuario().getFullName() : null,
                p.isEliminado(),
                p.getFechaEliminacion(),
                p.getEliminadoPor()
            ));
        }

        for (PedidoTienda p : servicioPedidosOperativos.obtenerPedidosTiendaIncluyendoEliminados()) {
            pedidos.add(new PedidoAdminVista(
                p.getId(),
                "Tienda",
                p.getClienteNombre(),
                p.getEmail(),
                p.getTelefono(),
                p.getEstado() != null ? p.getEstado().name() : null,
                p.getFechaCreacion(),
                p.getTotal(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                p.getResumenProductos(),
                null,
                p.isEliminado(),
                p.getFechaEliminacion(),
                p.getEliminadoPor()
            ));
        }

        pedidos.sort(Comparator.comparing(PedidoAdminVista::fechaCreacion, Comparator.nullsLast(Comparator.reverseOrder())));
        return pedidos;
    }

    @GetMapping("/admin/crear-categoria")
    public String crearCategoria(Model model, Authentication authentication) {
        model.addAttribute("categoria", new Categoria());
        model.addAttribute("pageTitle", "Maxcopias | Crear categoría");
        Usuario currentUsuario = userService.findRequiredByEmail(authentication.getName());
        model.addAttribute("currentUsuario", currentUsuario);
        return "admin/crearcategoria";
    }

    @PostMapping("/admin/crear-categoria")
    public String guardarCategoria(@RequestParam("nombre") String nombre, 
                                   @RequestParam(value = "descripcion", required = false) String descripcion,
                                   Model model, Authentication authentication) {
        try {
            Categoria categoria = new Categoria();
            categoria.setNombre(nombre.trim());
            if (descripcion != null && !descripcion.trim().isEmpty()) {
                categoria.setDescripcion(descripcion.trim());
            }
            servicioTienda.guardarCategoria(categoria);
            return redirectAfterCatalogChange(authentication);
        } catch (Exception e) {
            model.addAttribute("error", "Error al crear categoría: " + e.getMessage());
            model.addAttribute("categoria", new Categoria());
            Usuario currentUsuario = userService.findRequiredByEmail(authentication.getName());
            model.addAttribute("currentUsuario", currentUsuario);
            model.addAttribute("pageTitle", "Maxcopias | Crear categoría");
            return "admin/crearcategoria";
        }
    }

@GetMapping("/admin/crear-producto")
    public String crearProducto(Model model, Authentication authentication) {
        Producto nuevoProducto = new Producto();
        model.addAttribute("producto", nuevoProducto);
        model.addAttribute("pageTitle", "Maxcopias | Crear producto");
        model.addAttribute("categorias", servicioTienda.obtenerTodasCategorias());
        model.addAttribute("catalogReturnUrl", catalogReturnUrl(authentication));
        model.addAttribute("catalogReturnLabel", catalogReturnLabel(authentication));

        Usuario currentUsuario = userService.findRequiredByEmail(authentication.getName());
        model.addAttribute("currentUsuario", currentUsuario);
        return "admin/crearproducto";
    }

@PostMapping("/admin/crear-producto")
    public String guardarNuevoProducto(@RequestParam("nombre") String nombre, @RequestParam("descripcion") String descripcion,
                                      @RequestParam("stock") Integer stock, @RequestParam("precio") BigDecimal precio,
                                      @RequestParam("categoriaId") Long categoriaId,
                                      @RequestParam(value = "categoriaOpcionalId", required = false) Long categoriaOpcionalId, 
                                      Model model, Authentication authentication) {
        try {
            Producto producto = new Producto();
            producto.setNombre(nombre);
            producto.setDescripcion(descripcion);
            producto.setStock(stock);
            producto.setPrecio(precio);
            
            Categoria categoriaPrincipal = servicioTienda.obtenerCategoriaObligatoria(categoriaId);
            producto.clearCategorias();
            producto.addCategoria(categoriaPrincipal);
            
            if (categoriaOpcionalId != null) {
                Categoria categoriaOpcional = servicioTienda.obtenerCategoriaPorId(categoriaOpcionalId);
                if (categoriaOpcional != null && !categoriaOpcionalId.equals(categoriaId)) {
                    producto.addCategoria(categoriaOpcional);
                }
            }
            
            servicioTienda.guardarProducto(producto);
            return redirectAfterCatalogChange(authentication);
        } catch (Exception e) {
            model.addAttribute("error", "Error al crear producto: " + e.getMessage());
            Usuario currentUsuario = userService.findRequiredByEmail(authentication.getName());
            model.addAttribute("currentUsuario", currentUsuario);
            model.addAttribute("pageTitle", "Maxcopias | Crear producto");
            model.addAttribute("categorias", servicioTienda.obtenerTodasCategorias());
            model.addAttribute("catalogReturnUrl", catalogReturnUrl(authentication));
            model.addAttribute("catalogReturnLabel", catalogReturnLabel(authentication));
            return "admin/crearproducto";
        }
    }

@GetMapping("/editarstock/{id}")
    public String editarStock(@PathVariable Long id, Model model, Authentication authentication) {
        Producto producto = servicioTienda.obtenerProductoPorId(id);
        model.addAttribute("producto", producto);
        model.addAttribute("categorias", servicioTienda.obtenerTodasCategorias());
        model.addAttribute("pageTitle", "Maxcopias | Editar producto");
        model.addAttribute("catalogReturnUrl", catalogReturnUrl(authentication));
        model.addAttribute("catalogReturnLabel", catalogReturnLabel(authentication));
        Usuario currentUsuario = userService.findRequiredByEmail(authentication.getName());
        model.addAttribute("currentUsuario", currentUsuario);
        return "admin/editarstock";
    }

@PostMapping("/admin/update-producto/{id}")
    public String updateProducto(@PathVariable Long id, 
                                @RequestParam("nombre") String nombre,
                                @RequestParam("descripcion") String descripcion,
                                @RequestParam("stock") Integer stock,
                                @RequestParam("precio") BigDecimal precio,
                                @RequestParam("categoriaId") Long categoriaId,
                                @RequestParam(value="categoriaOpcionalId", required=false) Long categoriaOpcionalId,
                                Authentication authentication) {
        Producto existingProducto = servicioTienda.obtenerProductoPorId(id);
        existingProducto.setNombre(nombre);
        existingProducto.setDescripcion(descripcion);
        existingProducto.setStock(stock);
        existingProducto.setPrecio(precio);
        
        existingProducto.clearCategorias();
        Categoria catPrincipal = servicioTienda.obtenerCategoriaObligatoria(categoriaId);
        existingProducto.addCategoria(catPrincipal);
        
        if (categoriaOpcionalId != null) {
            Categoria catOpcional = servicioTienda.obtenerCategoriaPorId(categoriaOpcionalId);
            if (catOpcional != null && !catOpcional.getId().equals(categoriaId)) {
                existingProducto.addCategoria(catOpcional);
            }
        }
        
        servicioTienda.guardarProducto(existingProducto);
        return redirectAfterCatalogChange(authentication);
    }

    /**
     * Elimina categoría desde admin panel.
     */
    @DeleteMapping("/admin/delete-categoria/{id}")
    public @ResponseBody Map<String, String> deleteCategoria(@PathVariable Long id, Authentication authentication) {
        try {
            List<Long> productosIds = servicioTienda.verificarProductosEnCategoria(id);
            if (!productosIds.isEmpty()) {
                return Map.of("success", "false", "message", "No se puede eliminar. Categoría tiene productos asociados: " + productosIds);
            }
            repositorioCategoria.deleteById(id);
            return Map.of("success", "true", "message", "Categoría eliminada correctamente.");
        } catch (IllegalArgumentException e) {
            return Map.of("success", "false", "message", e.getMessage());
        } catch (Exception e) {
            return Map.of("success", "false", "message", "Error al eliminar categoría: " + e.getMessage());
        }
    }

    /**
     * Elimina producto desde admin panel.
     */
    @DeleteMapping("/admin/delete-producto/{id}")
    public @ResponseBody Map<String, String> deleteProducto(@PathVariable Long id, Authentication authentication) {
        try {
            servicioTienda.eliminarProductoPorId(id);
            return Map.of("success", "true", "message", "Producto eliminado correctamente.");
        } catch (Exception e) {
            return Map.of("success", "false", "message", "Error al eliminar producto: " + e.getMessage());
        }
    }

    @DeleteMapping("/admin/delete-user/{id}")
    public @ResponseBody Map<String, String> deleteUser(@PathVariable Long id, Authentication authentication) {
        try {
            Usuario currentUsuario = userService.findRequiredByEmail(authentication.getName());

            if (currentUsuario.getId().equals(id)) {
                return Map.of("success", "false", "message", "No puedes eliminar tu propio usuario administrador.");
            }

            if (!userRepository.existsById(id)) {
                return Map.of("success", "false", "message", "El usuario ya no existe.");
            }

            userRepository.deleteById(id);
            return Map.of("success", "true", "message", "Usuario eliminado correctamente.");
        } catch (Exception e) {
            return Map.of("success", "false", "message", "No se ha podido eliminar el usuario. Revisa si tiene pedidos asociados.");
        }
    }

    @PostMapping("/admin/pedidos/copisteria/{id}/eliminar")
    public String eliminarPedidoCopisteriaAdmin(@PathVariable Long id, Authentication authentication) {
        servicioPedidosOperativos.eliminarPedidoCopisteria(id, authentication.getName());
        return "redirect:/admin";
    }

    @PostMapping("/admin/pedidos/tienda/{id}/eliminar")
    public String eliminarPedidoTiendaAdmin(@PathVariable Long id, Authentication authentication) {
        servicioPedidosOperativos.eliminarPedidoTienda(id, authentication.getName());
        return "redirect:/admin";
    }

    @PostMapping("/admin/pedidos/copisteria/{id}/estado")
    public String cambiarEstadoCopisteriaAdmin(@PathVariable Long id, @RequestParam com.maxcopias.model.EstadoPedidoCopisteria estado) {
        servicioPedidosOperativos.cambiarEstadoCopisteria(id, estado);
        return "redirect:/admin?tab=orders";
    }

    @PostMapping("/admin/pedidos/tienda/{id}/estado")
    public String cambiarEstadoTiendaAdmin(@PathVariable Long id, @RequestParam com.maxcopias.model.EstadoPedidoTienda estado) {
        servicioPedidosOperativos.cambiarEstadoTienda(id, estado);
        return "redirect:/admin?tab=orders";
    }

    @PostMapping("/admin/pedidos/copisteria/estado/bulk")
    @ResponseBody
    public Map<String, String> cambiarEstadoCopisteriaBulkAdmin(@RequestParam List<Long> ids, @RequestParam com.maxcopias.model.EstadoPedidoCopisteria estado) {
        try {
            servicioPedidosOperativos.cambiarEstadoCopisteriaBulk(ids, estado);
            return Map.of("success", "true", "message", "Estado actualizado correctamente.");
        } catch (Exception e) {
            return Map.of("success", "false", "message", e.getMessage());
        }
    }

    @PostMapping("/admin/pedidos/tienda/estado/bulk")
    @ResponseBody
    public Map<String, String> cambiarEstadoTiendaBulkAdmin(@RequestParam List<Long> ids, @RequestParam com.maxcopias.model.EstadoPedidoTienda estado) {
        try {
            servicioPedidosOperativos.cambiarEstadoTiendaBulk(ids, estado);
            return Map.of("success", "true", "message", "Estado actualizado correctamente.");
        } catch (Exception e) {
            return Map.of("success", "false", "message", e.getMessage());
        }
    }

    private void populatePersonalAreaModel(Model model, Usuario currentUsuario, FormularioActualizarPerfil profileForm, boolean updated) {
        model.addAttribute("currentUsuario", currentUsuario);
        model.addAttribute("profileForm", profileForm);
        model.addAttribute("profileUpdated", updated);
        model.addAttribute("pageTitle", "Maxcopias | Area personal");
    }

    private String redirectAfterCatalogChange(Authentication authentication) {
        boolean isWorker = authentication.getAuthorities().stream()
            .anyMatch(authority -> "ROLE_WORKER".equals(authority.getAuthority()));
        boolean isAdmin = authentication.getAuthorities().stream()
            .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));

        return isWorker && !isAdmin ? "redirect:/worker/productos" : "redirect:/admin?updated=true";
    }

    private String catalogReturnUrl(Authentication authentication) {
        boolean isWorker = authentication.getAuthorities().stream()
            .anyMatch(authority -> "ROLE_WORKER".equals(authority.getAuthority()));
        boolean isAdmin = authentication.getAuthorities().stream()
            .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));

        return isWorker && !isAdmin ? "/worker/productos" : "/admin";
    }

    private String catalogReturnLabel(Authentication authentication) {
        boolean isWorker = authentication.getAuthorities().stream()
            .anyMatch(authority -> "ROLE_WORKER".equals(authority.getAuthority()));
        boolean isAdmin = authentication.getAuthorities().stream()
            .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));

        return isWorker && !isAdmin ? "Volver a productos" : "Volver al panel";
    }
}

