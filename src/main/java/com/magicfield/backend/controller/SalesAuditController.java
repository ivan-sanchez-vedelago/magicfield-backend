package com.magicfield.backend.controller;

import com.magicfield.backend.entity.SalesAudit;
import com.magicfield.backend.repository.SalesAuditRepository;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/sales-audit")
@CrossOrigin(origins = "*")
public class SalesAuditController {

    private final SalesAuditRepository salesAuditRepository;

    public SalesAuditController(SalesAuditRepository salesAuditRepository) {
        this.salesAuditRepository = salesAuditRepository;
    }

    /**
     * Obtener todas las auditorías de venta (ordenadas por fecha más reciente)
     */
    @GetMapping
    public List<SalesAudit> getAllAudits() {
        return salesAuditRepository.findAllByOrderBySaleDateDesc();
    }

    /**
     * Obtener auditorías por email del cliente
     */
    @GetMapping("/customer/{email}")
    public List<SalesAudit> getByCustomerEmail(@PathVariable String email) {
        return salesAuditRepository.findByCustomerEmail(email);
    }

    /**
     * Obtener auditorías por producto
     */
    @GetMapping("/product/{productId}")
    public List<SalesAudit> getByProduct(@PathVariable UUID productId) {
        return salesAuditRepository.findByProductId(productId);
    }

    /**
     * Obtener auditorías en un rango de fechas
     * Parámetros query:
     * - startDate: yyyy-MM-dd'T'HH:mm:ss
     * - endDate: yyyy-MM-dd'T'HH:mm:ss
     */
    @GetMapping("/range")
    public List<SalesAudit> getByDateRange(
            @RequestParam LocalDateTime startDate,
            @RequestParam LocalDateTime endDate
    ) {
        return salesAuditRepository.findBySaleDateBetween(startDate, endDate);
    }

    /**
     * Obtener auditorías por estado
     */
    @GetMapping("/status/{status}")
    public List<SalesAudit> getByStatus(@PathVariable String status) {
        return salesAuditRepository.findByStatus(status);
    }

    /**
     * Obtener una auditoría específica por ID
     */
    @GetMapping("/{id}")
    public SalesAudit getById(@PathVariable UUID id) {
        return salesAuditRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Auditoría no encontrada"));
    }

    /**
     * Obtener todos los items de una misma orden/compra
     */
    @GetMapping("/order/{orderId}")
    public List<SalesAudit> getByOrderId(@PathVariable UUID orderId) {
        return salesAuditRepository.findByOrderId(orderId);
    }
}
