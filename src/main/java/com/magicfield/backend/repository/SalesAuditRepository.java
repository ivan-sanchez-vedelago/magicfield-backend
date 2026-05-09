package com.magicfield.backend.repository;

import com.magicfield.backend.entity.SalesAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface SalesAuditRepository extends JpaRepository<SalesAudit, UUID> {

    /**
     * Obtener auditorías de venta por cliente (email)
     */
    List<SalesAudit> findByCustomerEmail(String customerEmail);

    /**
     * Obtener auditorías de venta por usuario ID
     */
    List<SalesAudit> findByUserIdOrderBySaleDateDesc(UUID userId);

    /**
     * Obtener auditorías de venta por producto
     */
    List<SalesAudit> findByProductId(UUID productId);

    /**
     * Obtener auditorías de venta en un rango de fechas
     */
    List<SalesAudit> findBySaleDateBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Obtener auditorías de venta por estado
     */
    List<SalesAudit> findByStatus(String status);

    /**
     * Obtener todas las auditorías ordenadas por fecha (más recientes primero)
     */
    List<SalesAudit> findAllByOrderBySaleDateDesc();

    /**
     * Obtener todos los items de una misma orden/compra
     */
    List<SalesAudit> findByOrderId(UUID orderId);

    /**
     * Verificar si existe algún item PENDING para un producto dado
     */
    boolean existsByProductIdAndStatus(UUID productId, String status);
}
