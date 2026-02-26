package com.magicfield.backend.controller;

import org.springframework.web.bind.annotation.*;
import com.magicfield.backend.service.OrderService;
import com.magicfield.backend.dto.CheckoutRequest;

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
}
