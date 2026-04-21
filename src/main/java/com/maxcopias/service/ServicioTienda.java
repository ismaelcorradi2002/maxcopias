package com.maxcopias.service;

import com.maxcopias.model.Categoria;
import com.maxcopias.model.Producto;
import com.maxcopias.repository.RepositorioCategoria;
import com.maxcopias.repository.RepositorioProducto;
import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ServicioTienda {

    private final RepositorioProducto repositorioProducto;
    private final RepositorioCategoria repositorioCategoria;

    public ServicioTienda(RepositorioProducto repositorioProducto, RepositorioCategoria repositorioCategoria) {
        this.repositorioProducto = repositorioProducto;
        this.repositorioCategoria = repositorioCategoria;
    }

    @Transactional(readOnly = true)
    public List<Producto> obtenerTodosProductos() {
        return repositorioProducto.findAllByOrderByNombreAsc();
    }

    @Transactional(readOnly = true)
    public List<Categoria> obtenerTodasCategorias() {
        return repositorioCategoria.findAllByOrderByNombreAsc();
    }

    @Transactional(readOnly = true)
    public List<Producto> obtenerProductosPorCategoria(Long categoriaId) {
        return repositorioProducto.findDistinctByCategoriasIdOrderByNombreAsc(categoriaId);
    }

    @Transactional(readOnly = true)
    public Producto obtenerProductoPorId(Long productoId) {
        return obtenerProductoObligatorio(productoId);
    }

    @Transactional
    public Producto guardarProducto(Producto producto) {
        producto.setNombre(normalizarTexto(producto.getNombre()));
        producto.setDescripcion(normalizarTexto(producto.getDescripcion()));
        producto.setStock(producto.getStock() == null ? 0 : producto.getStock());
        producto.setPrecio(producto.getPrecio() == null ? BigDecimal.ZERO : producto.getPrecio());
        
        return repositorioProducto.save(producto);
    }

    @Transactional
    public Categoria guardarCategoria(Categoria categoria) {
        categoria.setNombre(normalizarTexto(categoria.getNombre()));
        categoria.setDescripcion(normalizarTexto(categoria.getDescripcion()));
        return repositorioCategoria.save(categoria);
    }

    @Transactional
    public Producto asignarCategoriasAProducto(Long productoId, List<Long> categoriaIds) {
        Producto producto = obtenerProductoObligatorio(productoId);
        producto.clearCategorias();

        if (categoriaIds == null || categoriaIds.isEmpty()) {
            return repositorioProducto.save(producto);
        }

        Set<Categoria> categorias = new LinkedHashSet<>(repositorioCategoria.findAllById(categoriaIds));
        if (categorias.size() != categoriaIds.size()) {
            throw new IllegalArgumentException("Una o varias categorias no existen en la base de datos.");
        }

        for (Categoria categoria : categorias) {
            producto.addCategoria(categoria);
        }

        return repositorioProducto.save(producto);
    }

    @Transactional(readOnly = true)
    public Producto obtenerProductoObligatorio(Long productoId) {
        return repositorioProducto.findById(productoId)
            .orElseThrow(() -> new IllegalArgumentException("No existe el producto indicado."));
    }

    @Transactional
    public Categoria obtenerCategoriaObligatoria(Long categoriaId) {
        return repositorioCategoria.findById(categoriaId)
            .orElseThrow(() -> new IllegalArgumentException("No existe la categoria indicada."));
    }

    @Transactional(readOnly = true)
    public Categoria obtenerCategoriaPorId(Long categoriaId) {
        return repositorioCategoria.findById(categoriaId).orElse(null);
    }

    private String normalizarTexto(String valor) {
        return StringUtils.hasText(valor) ? valor.trim() : "";
    }

    /**
     * Elimina producto por ID.
     */
    @Transactional
    public void eliminarProductoPorId(Long productoId) {
        if (!repositorioProducto.existsById(productoId)) {
            throw new IllegalArgumentException("El producto no existe.");
        }
        repositorioProducto.deleteById(productoId);
    }
}
