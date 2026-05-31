package com.magicfield.backend.service;

import com.magicfield.backend.entity.Image;
import com.magicfield.backend.entity.Product;
import com.magicfield.backend.entity.SalesAudit;
import com.magicfield.backend.repository.ImageRepository;
import com.magicfield.backend.repository.ProductRepository;
import com.magicfield.backend.repository.SalesAuditRepository;
import com.magicfield.backend.dto.CheckoutRequest;
import com.magicfield.backend.dto.CheckoutItemRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;


@Service
public class OrderService {

    private final ProductRepository productRepository;
    private final ProductService productService;
    private final EmailService emailService;
    private final SalesAuditRepository salesAuditRepository;
    private final ImageRepository imageRepository;
    private final ImageStorageService imageStorageService;

    @Value("${app.admin-email}")
    private String adminEmail;

    public OrderService(
            ProductRepository productRepository,
            ProductService productService,
            EmailService emailService,
            SalesAuditRepository salesAuditRepository,
            ImageRepository imageRepository,
            ImageStorageService imageStorageService
    ) {
        this.productRepository = productRepository;
        this.productService = productService;
        this.emailService = emailService;
        this.salesAuditRepository = salesAuditRepository;
        this.imageRepository = imageRepository;
        this.imageStorageService = imageStorageService;
    }

    /** Capitaliza la primera letra de cada palabra, dejando en minúscula las preposiciones comunes. */
    private static String toTitleCase(String input) {
        if (input == null || input.isBlank()) return input;
        Set<String> skipWords = Set.of("de", "del", "la", "las", "los", "el", "y", "a", "en");
        String[] words = input.trim().toLowerCase().split("\\s+");
        return Arrays.stream(words)
            .map(w -> skipWords.contains(w)
                ? w
                : Character.toUpperCase(w.charAt(0)) + w.substring(1))
            .collect(Collectors.joining(" "));
    }

    @Transactional
    public void checkout(CheckoutRequest request) {

        // Generar ID único para esta orden (para relacionar todos sus items)
        UUID orderId = UUID.randomUUID();

        StringBuilder orderText = new StringBuilder();

        // Formatear dirección de envío
        String deliveryType = request.getDeliveryType();
        String deliveryDescription;
        if ("RETIRO_RAMOS".equals(deliveryType)) {
            deliveryDescription = "Retiro en Ramos Mejia";
        } else if ("RETIRO_FRANCISCO".equals(deliveryType)) {
            deliveryDescription = "Retiro en Francisco Alvarez";
        } else if ("ENVIO_DOMICILIO".equals(deliveryType)) {
            deliveryDescription = "(Envío a domicilio) "
                + toTitleCase(request.getShippingStreet()) + " " + request.getShippingStreetNumber()
                + ", " + toTitleCase(request.getShippingCity()) + ", " + toTitleCase(request.getShippingProvince());
        } else if ("ENVIO_ANDREANI".equals(deliveryType)) {
            deliveryDescription = "(Envío a sucursal Andreani) "
                + toTitleCase(request.getShippingStreet()) + " " + request.getShippingStreetNumber()
                + ", " + toTitleCase(request.getShippingCity()) + ", " + toTitleCase(request.getShippingProvince());
        } else {
            deliveryDescription = deliveryType != null ? deliveryType : "No especificado";
        }

        String clienteName = toTitleCase(request.getCustomerName()) + " " + toTitleCase(request.getCustomerLastName());

        orderText.append("Nuevo pedido Magic Field\n\n");
        orderText.append("ID Orden: ").append(orderId).append("\n\n");
        orderText.append("Cliente: ").append(clienteName).append("\n");
        orderText.append("Dirección de envío: ").append(deliveryDescription).append("\n");
        orderText.append("Código Postal: ")
                 .append(request.getShippingPostalCode() != null ? request.getShippingPostalCode() : "N/A").append("\n");
        orderText.append("DNI: ")
                 .append(request.getCustomerDni() != null ? request.getCustomerDni() : "N/A").append("\n");
        orderText.append("Teléfono: ").append(request.getCustomerPhone()).append("\n");
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
            audit.setDeliveryType(request.getDeliveryType());
            audit.setCustomerDni(request.getCustomerDni());
            audit.setShippingStreet(request.getShippingStreet());
            audit.setShippingStreetNumber(request.getShippingStreetNumber());
            audit.setShippingCity(request.getShippingCity());
            audit.setShippingProvince(request.getShippingProvince());
            audit.setShippingPostalCode(request.getShippingPostalCode());
            audit.setPaymentMethod(request.getPaymentMethod());
            if (request.getUserId() != null && !request.getUserId().isEmpty()) {
                audit.setUserId(UUID.fromString(request.getUserId()));
            }
            audit.setStatus("PENDING");
            salesAuditRepository.save(audit);
        }

        orderText.append("\nCosto Total: $").append(total);

        String paymentLabel = "EFECTIVO".equals(request.getPaymentMethod()) ? "Efectivo" : "Transferencia";
        orderText.append("\n\nMétodo de pago: ").append(paymentLabel);

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

    public List<SalesAudit> getUserOrders(UUID userId) {
        return salesAuditRepository.findByUserIdOrderBySaleDateDesc(userId);
    }

    @Transactional
    public void finalizeOrder(UUID orderId) {
        List<SalesAudit> items = salesAuditRepository.findByOrderId(orderId);
        if (items.isEmpty()) {
            throw new RuntimeException("Orden no encontrada: " + orderId);
        }

        boolean allPending = items.stream().allMatch(a -> "PENDING".equals(a.getStatus()));
        if (!allPending) {
            throw new IllegalStateException("Solo se pueden finalizar órdenes en estado PENDING");
        }

        // Marcar todos los items como COMPLETED
        items.forEach(a -> a.setStatus("COMPLETED"));
        salesAuditRepository.saveAll(items);

        // Eliminar productos con stock=0 que ya no tienen otros PENDING
        items.stream()
                .map(SalesAudit::getProductId)
                .distinct()
                .forEach(productId -> {
                    productRepository.findById(productId).ifPresent(product -> {
                        if (product.getStock() == 0
                                && !salesAuditRepository.existsByProductIdAndStatus(productId, "PENDING")) {
                            // Eliminar imágenes si no es SIN (los SIN usan Scryfall)
                            if (product.getCategory() == null || !"SIN".equals(product.getCategory().getShortName())) {
                                List<Image> images = imageRepository.findByProductId(productId);
                                imageRepository.deleteByProductId(productId);
                                images.forEach(image -> {
                                    try {
                                        imageStorageService.deleteByUrl(image.getUrl());
                                    } catch (Exception e) {
                                        System.err.println("Error al eliminar imagen de Firebase: " + image.getUrl());
                                    }
                                });
                            }
                            productRepository.delete(product);
                        }
                    });
                });
    }

    @Transactional
    public void cancelOrder(UUID orderId, UUID requestingUserId, boolean isAdmin) {
        List<SalesAudit> items = salesAuditRepository.findByOrderId(orderId);
        if (items.isEmpty()) {
            throw new RuntimeException("Orden no encontrada: " + orderId);
        }

        boolean allPending = items.stream().allMatch(a -> "PENDING".equals(a.getStatus()));
        if (!allPending) {
            throw new IllegalStateException("Solo se pueden cancelar órdenes en estado PENDING");
        }

        // Verificar autorización si no es admin
        if (!isAdmin) {
            UUID orderUserId = items.get(0).getUserId();
            if (orderUserId == null || !orderUserId.equals(requestingUserId)) {
                throw new AccessDeniedException("No tenés permiso para cancelar esta orden");
            }
        }

        // Marcar todos los items como CANCELED
        items.forEach(a -> a.setStatus("CANCELED"));
        salesAuditRepository.saveAll(items);

        // Restaurar stock de cada producto
        items.forEach(item -> {
            productRepository.findById(item.getProductId()).ifPresent(product -> {
                product.setStock(product.getStock() + item.getQuantity());
                productRepository.save(product);
            });
        });
    }
}
