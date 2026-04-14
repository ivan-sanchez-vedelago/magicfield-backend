package com.magicfield.backend.controller;

import org.springframework.web.bind.annotation.*;
import com.magicfield.backend.service.OrderService;
import com.magicfield.backend.dto.CheckoutRequest;
import com.magicfield.backend.entity.SalesAudit;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import java.util.List;
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
    public ResponseEntity<List<SalesAudit>> getUserOrders(@PathVariable UUID userId) {
        try {
            List<SalesAudit> orders = orderService.getUserOrders(userId);
            return new ResponseEntity<>(orders, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
