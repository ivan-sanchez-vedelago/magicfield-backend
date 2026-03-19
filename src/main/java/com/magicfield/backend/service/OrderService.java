package com.magicfield.backend.service;

import com.magicfield.backend.entity.Product;
import com.magicfield.backend.repository.ProductRepository;
import com.magicfield.backend.dto.CheckoutRequest;
import com.magicfield.backend.dto.CheckoutItemRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
public class OrderService {

    private final ProductRepository productRepository;
    private final ProductService productService;
    private final EmailService emailService;

    @Value("${app.admin-email}")
    private String adminEmail;

    public OrderService(
            ProductRepository productRepository,
            ProductService productService,
            EmailService emailService
    ) {
        this.productRepository = productRepository;
        this.productService = productService;
        this.emailService = emailService;
    }

    @Transactional
    public void checkout(CheckoutRequest request) {

        StringBuilder orderText = new StringBuilder();

        orderText.append("Nuevo pedido Magic Field\n\n");
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
