package com.magicfield.backend.controller;

import org.springframework.web.bind.annotation.*;
import com.magicfield.backend.service.OrderService;
import com.magicfield.backend.dto.CheckoutRequest;
import com.magicfield.backend.entity.SalesAudit;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/checkout")
    public void checkout(@RequestBody CheckoutRequest request) {
        orderService.checkout(request);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<SalesAudit>> getUserOrders(@PathVariable String userId) {
        try {
            UUID userIdUUID;
            try {
                userIdUUID = UUID.fromString(userId);
            } catch (IllegalArgumentException e) {
                return new ResponseEntity<>(List.of(), HttpStatus.OK);
            }
            
            List<SalesAudit> orders = orderService.getUserOrders(userIdUUID);
            return new ResponseEntity<>(orders, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/{orderId}/finalize")
    public ResponseEntity<Void> finalizeOrder(@PathVariable String orderId) {
        try {
            orderService.finalizeOrder(UUID.fromString(orderId));
            return ResponseEntity.ok().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<Void> cancelOrder(
            @PathVariable String orderId,
            @RequestBody(required = false) Map<String, String> body
    ) {
        try {
            String userIdStr = body != null ? body.get("userId") : null;
            boolean isAdmin = body != null && "true".equals(body.get("isAdmin"));
            UUID requestingUserId = null;
            if (userIdStr != null && !userIdStr.isBlank()) {
                try {
                    requestingUserId = UUID.fromString(userIdStr);
                } catch (IllegalArgumentException ignored) {}
            }
            orderService.cancelOrder(UUID.fromString(orderId), requestingUserId, isAdmin);
            return ResponseEntity.ok().build();
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
