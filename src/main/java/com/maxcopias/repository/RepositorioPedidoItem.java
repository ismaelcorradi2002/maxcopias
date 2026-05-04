package com.maxcopias.repository;

import com.maxcopias.model.PedidoItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepositorioPedidoItem extends JpaRepository<PedidoItem, Long> {
    List<PedidoItem> findAllByPedidoId(Long pedidoId);
}
