package com.maxcopias.repository;

import com.maxcopias.model.Carrito;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepositorioCarrito extends JpaRepository<Carrito, Long> {

    @EntityGraph(attributePaths = {"items", "items.producto", "items.producto.categorias", "usuario"})
    Optional<Carrito> findByUsuarioIdAndActivoTrue(Long usuarioId);

    @EntityGraph(attributePaths = {"items", "items.producto", "items.producto.categorias", "usuario"})
    Optional<Carrito> findBySessionIdAndActivoTrue(String sessionId);
}
