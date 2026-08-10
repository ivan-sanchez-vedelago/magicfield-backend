package com.magicfield.backend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "sales_audit")
public class SalesAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Información del producto
    @Column(nullable = false)
    private UUID productId;

    @Column(nullable = false)
    private String productName;

    // Snapshot de datos del single al momento de la venta (mismo criterio que productName:
    // strings planos, no FK, para que el registro de auditoría no cambie si luego se
    // editan/borran las tablas de condición/idioma/finish). Null para no-singles.
    @Column
    private String set;

    @Column
    private String collectorNumber;

    @Column
    private String conditionName;

    @Column
    private String languageName;

    @Column
    private String finishName;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private BigDecimal subtotal;

    // Información del cliente
    @Column(nullable = false)
    private String customerName;

    @Column(nullable = false)
    private String customerLastName;

    @Column(nullable = false)
    private String customerEmail;

    @Column(nullable = false)
    private String customerPhone;

    // Información de la venta
    @Column(nullable = true, updatable = false)
    private UUID orderId; // Relaciona todos los items de la misma compra

    @Column(nullable = true)
    private UUID userId; // ID del usuario si está logueado, null si no

    @Column(nullable = false, updatable = false)
    private LocalDateTime saleDate;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(nullable = false)
    private String status; // COMPLETED, PENDING, CANCELLED

    // Información de envío
    @Column(nullable = true)
    private String deliveryType;

    @Column(nullable = true)
    private String customerDni;

    @Column(nullable = true)
    private String shippingStreet;

    @Column(nullable = true)
    private String shippingStreetNumber;

    @Column(nullable = true)
    private String shippingCity;

    @Column(nullable = true)
    private String shippingProvince;

    @Column(nullable = true)
    private String shippingPostalCode;

    @Column(nullable = true)
    private String paymentMethod;

    public SalesAudit() {
        this.saleDate = LocalDateTime.now();
        this.status = "PENDING";
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getProductId() {
        return productId;
    }

    public void setProductId(UUID productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getSet() {
        return set;
    }

    public void setSet(String set) {
        this.set = set;
    }

    public String getCollectorNumber() {
        return collectorNumber;
    }

    public void setCollectorNumber(String collectorNumber) {
        this.collectorNumber = collectorNumber;
    }

    public String getConditionName() {
        return conditionName;
    }

    public void setConditionName(String conditionName) {
        this.conditionName = conditionName;
    }

    public String getLanguageName() {
        return languageName;
    }

    public void setLanguageName(String languageName) {
        this.languageName = languageName;
    }

    public String getFinishName() {
        return finishName;
    }

    public void setFinishName(String finishName) {
        this.finishName = finishName;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerLastName() {
        return customerLastName;
    }

    public void setCustomerLastName(String customerLastName) {
        this.customerLastName = customerLastName;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public LocalDateTime getSaleDate() {
        return saleDate;
    }

    public void setSaleDate(LocalDateTime saleDate) {
        this.saleDate = saleDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDeliveryType() { return deliveryType; }
    public void setDeliveryType(String deliveryType) { this.deliveryType = deliveryType; }

    public String getCustomerDni() { return customerDni; }
    public void setCustomerDni(String customerDni) { this.customerDni = customerDni; }

    public String getShippingStreet() { return shippingStreet; }
    public void setShippingStreet(String shippingStreet) { this.shippingStreet = shippingStreet; }

    public String getShippingStreetNumber() { return shippingStreetNumber; }
    public void setShippingStreetNumber(String shippingStreetNumber) { this.shippingStreetNumber = shippingStreetNumber; }

    public String getShippingCity() { return shippingCity; }
    public void setShippingCity(String shippingCity) { this.shippingCity = shippingCity; }

    public String getShippingProvince() { return shippingProvince; }
    public void setShippingProvince(String shippingProvince) { this.shippingProvince = shippingProvince; }

    public String getShippingPostalCode() { return shippingPostalCode; }
    public void setShippingPostalCode(String shippingPostalCode) { this.shippingPostalCode = shippingPostalCode; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
}
