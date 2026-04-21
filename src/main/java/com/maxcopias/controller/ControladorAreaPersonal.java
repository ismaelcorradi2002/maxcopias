package com.maxcopias.controller;

/**
 * Controlador del área personal del usuario (perfil, pedidos).
 */
import com.maxcopias.dto.FormularioActualizarPerfil;
import com.maxcopias.service.ServicioPedidoCopisteria;
import com.maxcopias.model.Usuario;
import com.maxcopias.service.ServicioUsuario;
import com.maxcopias.model.Rol;
import com.maxcopias.repository.RepositorioCategoria;
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
import com.maxcopias.model.Categoria;
import java.math.BigDecimal;
import com.maxcopias.service.ServicioTienda;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.http.HttpStatus;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;

@Controller
public class ControladorAreaPersonal {

    private final ServicioUsuario userService;
    private final ServicioPedidoCopisteria copisteriaOrderService;

    /**
     * Inyecta servicios de usuario y pedidos.
     */
    private final RepositorioUsuario userRepository;
    private final RepositorioCategoria repositorioCategoria;

    /**
     * Inyecta servicios de usuario/pedidos y repositorio.
     */
    private final ServicioTienda servicioTienda;

    public ControladorAreaPersonal(ServicioUsuario userService, ServicioPedidoCopisteria copisteriaOrderService, RepositorioUsuario userRepository, ServicioTienda servicioTienda, RepositorioCategoria repositorioCategoria) {
        this.repositorioCategoria = repositorioCategoria;
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

    @GetMapping("/admin/api/categorias")
    @ResponseBody
    public List<Categoria> getCategoriasApi() {
        return servicioTienda.obtenerTodasCategorias();
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
            return "redirect:/admin";
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
            return "redirect:/admin?created=true";
        } catch (Exception e) {
            model.addAttribute("error", "Error al crear producto: " + e.getMessage());
            Usuario currentUsuario = userService.findRequiredByEmail(authentication.getName());
            model.addAttribute("currentUsuario", currentUsuario);
            model.addAttribute("pageTitle", "Maxcopias | Crear producto");
            model.addAttribute("categorias", servicioTienda.obtenerTodasCategorias());
            return "admin/crearproducto";
        }
    }

@GetMapping("/editarstock/{id}")
    public String editarStock(@PathVariable Long id, Model model, Authentication authentication) {
        Producto producto = servicioTienda.obtenerProductoPorId(id);
        model.addAttribute("producto", producto);
        model.addAttribute("categorias", servicioTienda.obtenerTodasCategorias());
        model.addAttribute("pageTitle", "Maxcopias | Editar stock");
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
        return "redirect:/admin?updated=true";
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

    private void populatePersonalAreaModel(Model model, Usuario currentUsuario, FormularioActualizarPerfil profileForm, boolean updated) {
        model.addAttribute("currentUsuario", currentUsuario);
        model.addAttribute("profileForm", profileForm);
        model.addAttribute("profileUpdated", updated);
        model.addAttribute("pageTitle", "Maxcopias | Area personal");
    }
}

