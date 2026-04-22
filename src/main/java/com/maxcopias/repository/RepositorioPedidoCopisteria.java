package com.maxcopias.repository;

import com.maxcopias.model.PedidoCopisteria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio JPA para pedidos de copisteria.
 */
@Repository
public interface RepositorioPedidoCopisteria extends JpaRepository<PedidoCopisteria, Long> {
}

