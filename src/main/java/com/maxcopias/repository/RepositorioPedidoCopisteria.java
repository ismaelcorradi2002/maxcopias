package com.maxcopias.repository;

import com.maxcopias.model.PedidoCopisteria;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * Repositorio JPA para pedidos de copisteria.
 */
@Repository
public interface RepositorioPedidoCopisteria extends JpaRepository<PedidoCopisteria, Long> {
    List<PedidoCopisteria> findAllByOrderByFechaCreacionDesc();

    @Query("SELECT p FROM PedidoCopisteria p LEFT JOIN FETCH p.usuario ORDER BY p.fechaCreacion DESC")
    List<PedidoCopisteria> findAllWithUsuarioOrderByFechaCreacionDesc();
}

