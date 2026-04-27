package com.maxcopias.repository;

import com.maxcopias.model.PedidoCopisteria;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repositorio JPA para pedidos de copisteria.
 */
@Repository
public interface RepositorioPedidoCopisteria extends JpaRepository<PedidoCopisteria, Long> {
    List<PedidoCopisteria> findAllByOrderByFechaCreacionDesc();
    boolean existsByCodigoRecoger(String codigoRecoger);

    @Query("SELECT p FROM PedidoCopisteria p LEFT JOIN FETCH p.usuario ORDER BY p.fechaCreacion DESC")
    List<PedidoCopisteria> findAllWithUsuarioOrderByFechaCreacionDesc();

    @Query("SELECT p FROM PedidoCopisteria p LEFT JOIN FETCH p.usuario WHERE p.id = :id")
    Optional<PedidoCopisteria> findByIdWithUsuario(@Param("id") Long id);
}

