package com.maxcopias.repository;

import com.maxcopias.model.Producto;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepositorioProducto extends JpaRepository<Producto, Long> {

    @EntityGraph(attributePaths = "categorias")
    List<Producto> findAllByOrderByNombreAsc();

    @Override
    @EntityGraph(attributePaths = "categorias")
    Optional<Producto> findById(Long id);

    @EntityGraph(attributePaths = "categorias")
    List<Producto> findDistinctByCategoriasIdOrderByNombreAsc(Long categoriaId);

    boolean existsByNombreIgnoreCase(String nombre);
}
