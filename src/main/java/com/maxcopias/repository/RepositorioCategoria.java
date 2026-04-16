package com.maxcopias.repository;

import com.maxcopias.model.Categoria;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepositorioCategoria extends JpaRepository<Categoria, Long> {

    @EntityGraph(attributePaths = "productos")
    List<Categoria> findAllByOrderByNombreAsc();

    Optional<Categoria> findByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCase(String nombre);
}
