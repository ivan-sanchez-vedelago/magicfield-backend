package com.magicfield.backend.service;

import com.magicfield.backend.entity.Product;
import com.magicfield.backend.entity.SalesAudit;
import com.magicfield.backend.repository.ProductRepository;
import com.magicfield.backend.repository.SalesAuditRepository;
import com.magicfield.backend.dto.CheckoutRequest;
import com.magicfield.backend.dto.CheckoutItemRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;


@Service
public class OrderService {

    private final ProductRepository productRepository;
    private final ProductService productService;
    private final EmailService emailService;
    private final SalesAuditRepository salesAuditRepository;

    @Value("${app.admin-email}")
    private String adminEmail;

    public OrderService(
            ProductRepository productRepository,
            ProductService productService,
            EmailService emailService,
            SalesAuditRepository salesAuditRepository
    ) {
        this.productRepository = productRepository;
        this.productService = productService;
        this.emailService = emailService;
        this.salesAuditRepository = salesAuditRepository;
    }

    @Transactional
    public void checkout(CheckoutRequest request) {

        // Generar ID único para esta orden (para relacionar todos sus items)
        UUID orderId = UUID.randomUUID();

        StringBuilder orderText = new StringBuilder();

        orderText.append("Nuevo pedido Magic Field\n\n");
        orderText.append("ID Orden: ").append(orderId).append("\n\n");
        orderText.append("Cliente:\n");
        orderText.append(request.getCustomerName())
                 .append(" ")
                 .append(request.getCustomerLastName()).append("\n");
        orderText.append("Telefono: ").append(request.getCustomerPhone()).append("\n");
        orderText.append("Email: ").append(request.getCustomerEmail()).append("\n\n");

        orderText.append("Productos:\n");

        double total = 0;

        for (CheckoutItemRequest item : request.getItems()) {

            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

            if (product.getStock() < item.getQuantity()) {
                throw new RuntimeException("Stock insuficiente para " + product.getName());
            }

            double subtotal = product.getPrice().intValue() * item.getQuantity();
            total += subtotal;

            orderText.append("- ")
                    .append(product.getName())
                    .append(" x")
                    .append(item.getQuantity())
                    .append(" = $")
                    .append(subtotal)
                    .append("\n");

            // DESCUESTO STOCK (usa tu servicio existente)
            productService.decreaseStock(product.getId(), item.getQuantity());

            // GUARDAR AUDITORÍA DE VENTA
            SalesAudit audit = new SalesAudit();
            audit.setOrderId(orderId);  // ← Asocia este item con la orden
            audit.setProductId(product.getId());
            audit.setProductName(product.getName());
            audit.setQuantity(item.getQuantity());
            audit.setUnitPrice(product.getPrice());
            audit.setSubtotal(BigDecimal.valueOf(subtotal));
            audit.setCustomerName(request.getCustomerName());
            audit.setCustomerLastName(request.getCustomerLastName());
            audit.setCustomerEmail(request.getCustomerEmail());
            audit.setCustomerPhone(request.getCustomerPhone());
            audit.setStatus("COMPLETED");
            salesAuditRepository.save(audit);
        }

        orderText.append("\nTOTAL: $").append(total);

        // EMAIL ADMIN
        try {
            emailService.send(
                adminEmail,
                "Nuevo pedido recibido",
                orderText.toString()
            );
        } catch (Exception e) {
            System.err.println("Error enviando email admin");
            e.printStackTrace();
        }

        // EMAIL CLIENTE
        try {
            emailService.send(
                request.getCustomerEmail(),
                "Pedido confirmado",
                "Hola " + request.getCustomerName() +
                "!\n\nRecibimos tu pedido correctamente."
            );
        } catch (Exception e) {
            System.err.println("Error enviando email cliente");
            e.printStackTrace();
        }
    }
}
