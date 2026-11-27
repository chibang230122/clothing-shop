package com.example.clothingshop.controller;

import com.example.clothingshop.service.OrderPaymentService;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SePayWebhookController {

    private final OrderPaymentService paymentService;

    @Value("${app.payment.mode:DEMO}")
    private String paymentMode;
    @Value("${app.payment.sepay-webhook-api-key:}")
    private String webhookApiKey;

    public SePayWebhookController(OrderPaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/webhook/sepay")
    public ResponseEntity<Map<String, Object>> receive(
            @RequestBody JsonNode payload,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        if (!"SEPAY_TEST".equalsIgnoreCase(paymentMode)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("success", false));
        }
        if (webhookApiKey == null || webhookApiKey.isBlank()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("success", false));
        }
        String expected = "Apikey " + webhookApiKey;
        if (authorization == null || !MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8), authorization.getBytes(StandardCharsets.UTF_8))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("success", false));
        }

        String type = text(payload, "transferType");
        String code = text(payload, "code");
        String content = text(payload, "content");
        BigDecimal amount = payload.hasNonNull("transferAmount")
                ? payload.get("transferAmount").decimalValue() : BigDecimal.ZERO;
        boolean accepted = paymentService.processSePayIncoming(code, content, type, amount);
        // Return success for validly received events; unmatched/underpaid events are not marked PAID.
        return ResponseEntity.ok(Map.of("success", true, "matched", accepted));
    }

    private String text(JsonNode node, String field) {
        return node.hasNonNull(field) ? node.get(field).asText() : null;
    }
}
