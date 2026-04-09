package com.maxcopias.repository;

import com.maxcopias.model.PedidoCopisteria;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepositorioPedidoCopisteria extends JpaRepository<PedidoCopisteria, Long> {

    boolean existsByPickupCode(String pickupCode);

    @EntityGraph(attributePaths = "files")
    Optional<PedidoCopisteria> findByPickupCode(String pickupCode);

    @EntityGraph(attributePaths = "files")
    Optional<PedidoCopisteria> findByPickupCodeAndUserId(String pickupCode, Long userId);

    @EntityGraph(attributePaths = "files")
    List<PedidoCopisteria> findAllByUserIdOrderByCreatedAtDesc(Long userId);
}

