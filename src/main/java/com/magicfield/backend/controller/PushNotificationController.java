package com.magicfield.backend.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.magicfield.backend.service.PushNotificationService;
import com.magicfield.backend.dto.RegisterPushTokenRequest;

@RestController
@RequestMapping("/api/push-tokens")
@CrossOrigin(origins = "*")
public class PushNotificationController {

    private final PushNotificationService pushNotificationService;

    public PushNotificationController(PushNotificationService pushNotificationService) {
        this.pushNotificationService = pushNotificationService;
    }

    @PostMapping
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterPushTokenRequest request) {
        pushNotificationService.registerToken(request.getToken(), request.getPlatform());
        return ResponseEntity.ok().build();
    }
}
