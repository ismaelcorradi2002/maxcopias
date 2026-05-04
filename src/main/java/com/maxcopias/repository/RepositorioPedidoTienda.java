package com.maxcopias.repository;

import com.maxcopias.model.PedidoTienda;
import com.maxcopias.model.Usuario;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RepositorioPedidoTienda extends JpaRepository<PedidoTienda, Long> {
    @EntityGraph(attributePaths = {"usuario", "items", "items.producto"})
    List<PedidoTienda> findAllByEliminadoFalseOrderByFechaCreacionDesc();

    @EntityGraph(attributePaths = {"usuario", "items", "items.producto"})
    List<PedidoTienda> findAllByOrderByFechaCreacionDesc();

    List<PedidoTienda> findAllByEliminadoTrueOrderByFechaEliminacionDesc();

    @EntityGraph(attributePaths = {"usuario", "items", "items.producto"})
    Optional<PedidoTienda> findByIdAndEliminadoFalse(Long id);

    @EntityGraph(attributePaths = {"usuario", "items", "items.producto"})
    Optional<PedidoTienda> findById(Long id);

    @EntityGraph(attributePaths = {"usuario", "items", "items.producto"})
    List<PedidoTienda> findAllByUsuarioAndEliminadoFalseOrderByFechaCreacionDesc(Usuario usuario);

    boolean existsByCodigoPedido(String codigoPedido);
}
