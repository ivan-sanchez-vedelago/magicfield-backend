package com.magicfield.backend.repository;

import com.magicfield.backend.entity.SalesAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
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

    // --- Dashboard Stats Queries ---

    @Query("SELECT COUNT(DISTINCT s.orderId) FROM SalesAudit s WHERE s.saleDate >= :since")
    long countDistinctOrdersSince(@Param("since") LocalDateTime since);

    @Query("SELECT COALESCE(SUM(s.subtotal), 0) FROM SalesAudit s WHERE s.saleDate >= :since")
    BigDecimal sumRevenueSince(@Param("since") LocalDateTime since);

    @Query("SELECT COUNT(DISTINCT s.orderId) FROM SalesAudit s WHERE s.status = :status")
    long countDistinctOrdersByStatus(@Param("status") String status);

    @Query(value = "SELECT s.product_id, s.product_name, SUM(s.quantity) as total_qty " +
            "FROM sales_audit s GROUP BY s.product_id, s.product_name " +
            "ORDER BY total_qty DESC LIMIT 5", nativeQuery = true)
    List<Object[]> findTopProducts();
}
