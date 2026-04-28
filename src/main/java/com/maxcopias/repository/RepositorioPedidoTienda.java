package com.maxcopias.repository;

import com.maxcopias.model.PedidoTienda;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RepositorioPedidoTienda extends JpaRepository<PedidoTienda, Long> {
    List<PedidoTienda> findAllByEliminadoFalseOrderByFechaCreacionDesc();
    List<PedidoTienda> findAllByOrderByFechaCreacionDesc();
    List<PedidoTienda> findAllByEliminadoTrueOrderByFechaEliminacionDesc();
    Optional<PedidoTienda> findByIdAndEliminadoFalse(Long id);
}
