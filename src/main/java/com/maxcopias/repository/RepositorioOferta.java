package com.maxcopias.repository;

import com.maxcopias.model.Oferta;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RepositorioOferta extends JpaRepository<Oferta, Long> {
    @EntityGraph(attributePaths = {"producto", "productos", "categoria"})
    List<Oferta> findAllByOrderByFechaCreacionDesc();

    @EntityGraph(attributePaths = {"producto", "productos", "categoria"})
    List<Oferta> findAllByActivaTrueOrderByFechaCreacionDesc();

    @EntityGraph(attributePaths = {"producto", "productos", "categoria"})
    Optional<Oferta> findFirstByActivaTrueAndPrincipalHomeTrueOrderByFechaCreacionDesc();

    @Override
    @EntityGraph(attributePaths = {"producto", "productos", "categoria"})
    Optional<Oferta> findById(Long id);
}
