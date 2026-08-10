package com.magicfield.backend.service;

import com.magicfield.backend.entity.Product;
import com.magicfield.backend.entity.SalesAudit;
import com.magicfield.backend.repository.ProductRepository;
import com.magicfield.backend.repository.SalesAuditRepository;
import com.magicfield.backend.dto.CheckoutRequest;
import com.magicfield.backend.dto.CheckoutItemRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;


@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final ProductRepository productRepository;
    private final ProductService productService;
    private final EmailService emailService;
    private final SalesAuditRepository salesAuditRepository;
    private final PushNotificationService pushNotificationService;

    @Value("${app.admin-email}")
    private String adminEmail;

    public OrderService(
            ProductRepository productRepository,
            ProductService productService,
            EmailService emailService,
            SalesAuditRepository salesAuditRepository,
            PushNotificationService pushNotificationService
    ) {
        this.productRepository = productRepository;
        this.productService = productService;
        this.emailService = emailService;
        this.salesAuditRepository = salesAuditRepository;
        this.pushNotificationService = pushNotificationService;
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

    /**
     * Set, N° de coleccionista, condición, idioma y finish de un single, en ese orden --
     * mismo criterio y mismo orden que se muestra en carrito y en los paneles de pedidos,
     * para que el mail de confirmación diga exactamente lo mismo que ve el usuario/admin.
     * Vacío para no-singles.
     */
    private static String buildVariantSuffix(Product product) {
        if (product.getCategory() == null || !"SIN".equals(product.getCategory().getShortName())) {
            return "";
        }
        List<String> parts = new ArrayList<>();
        if (product.getSet() != null) parts.add(product.getSet());
        if (product.getCollectorNumber() != null) parts.add("#" + product.getCollectorNumber());
        if (product.getCondition() != null) parts.add(product.getCondition().getLongName());
        if (product.getLanguage() != null) parts.add(product.getLanguage().getLongName());
        if (product.getFinish() != null) parts.add(product.getFinish().getLongName());
        if (parts.isEmpty()) return "";
        return " (" + String.join(" · ", parts) + ")";
    }

    @Transactional
    public void checkout(CheckoutRequest request) {

        // Generar ID único para esta orden (para relacionar todos sus items)
        UUID orderId = UUID.randomUUID();

        StringBuilder orderTextAdmin = new StringBuilder();
        orderTextAdmin.append("Nuevo pedido Magic Field\n\n");

        StringBuilder orderTextClient = new StringBuilder();

        orderTextClient.append("Hola " + request.getCustomerName() +
            "\n¡Recibimos tu pedido correctamente! En breve nos pondremos en contacto para coordinar la entrega.\n\n");

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

        orderTextAdmin.append("ID Orden: ").append(orderId).append("\n\n");
        orderTextAdmin.append("Cliente: ").append(clienteName).append("\n");
        orderTextAdmin.append("Dirección de envío: ").append(deliveryDescription).append("\n");
        orderTextAdmin.append("Código Postal: ")
                 .append(request.getShippingPostalCode() != null ? request.getShippingPostalCode() : "N/A").append("\n");
        orderTextAdmin.append("DNI: ")
                 .append(request.getCustomerDni() != null ? request.getCustomerDni() : "N/A").append("\n");
        orderTextAdmin.append("Teléfono: ").append(request.getCustomerPhone()).append("\n");
        orderTextAdmin.append("Email: ").append(request.getCustomerEmail()).append("\n\n");
        
        orderTextAdmin.append("Método de pago: ").append(toTitleCase(request.getPaymentMethod()));

        orderTextAdmin.append("\nProductos:\n");
        orderTextClient.append("\nProductos:\n");

        double total = 0;

        for (CheckoutItemRequest item : request.getItems()) {

            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

            if (product.getStock() < item.getQuantity()) {
                throw new RuntimeException("Stock insuficiente para " + product.getName());
            }

            double subtotal = product.getPrice().intValue() * item.getQuantity();
            total += subtotal;

            String variantSuffix = buildVariantSuffix(product);

            orderTextAdmin.append("- ")
                    .append(product.getName())
                    .append(variantSuffix)
                    .append(" x")
                    .append(item.getQuantity())
                    .append(" = $")
                    .append(subtotal)
                    .append("\n");
            orderTextClient.append("- ")
                    .append(product.getName())
                    .append(variantSuffix)
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
            if (product.getCategory() != null && "SIN".equals(product.getCategory().getShortName())) {
                audit.setSet(product.getSet());
                audit.setCollectorNumber(product.getCollectorNumber());
                audit.setConditionName(product.getCondition() != null ? product.getCondition().getLongName() : null);
                audit.setLanguageName(product.getLanguage() != null ? product.getLanguage().getLongName() : null);
                audit.setFinishName(product.getFinish() != null ? product.getFinish().getLongName() : null);
            }
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

        orderTextAdmin.append("\nCosto Total: $").append(total);
        orderTextClient.append("\nCosto Total: $").append(total);

        orderTextClient.append("\n\n¡Gracias por comprar en Magic Field!");

        // EMAIL ADMIN
        try {
            emailService.send(
                adminEmail,
                "Nuevo pedido recibido",
                orderTextAdmin.toString()
            );
        } catch (Exception e) {
            log.error("[OrderService] Error enviando email admin para orderId={}: {}", orderId, e.getMessage());
        }

        // EMAIL CLIENTE
        try {
            emailService.send(
                request.getCustomerEmail(),
                "Pedido confirmado",
                orderTextClient.toString()
            );
        } catch (Exception e) {
            log.error("[OrderService] Error enviando email cliente email={}: {}", request.getCustomerEmail(), e.getMessage());
        }

        // PUSH NOTIFICATION ADMIN
        try {
            pushNotificationService.notifyNewOrder(
                "Nuevo pedido recibido",
                clienteName + " - $" + total
            );
        } catch (Exception e) {
            log.error("[OrderService] Error enviando push admin para orderId={}: {}", orderId, e.getMessage());
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

        // Los productos que hayan quedado en stock=0 no se eliminan: quedan
        // ocultos de las pantallas normales y pueden restaurarse desde admin.
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
