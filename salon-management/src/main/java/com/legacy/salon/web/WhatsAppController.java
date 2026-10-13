package com.legacy.salon.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.legacy.salon.model.MessageLog;
import com.legacy.salon.repo.MessageLogRepository;
import com.legacy.salon.service.WhatsAppService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class WhatsAppController {

    public record SendRequest(@NotBlank String to, @NotBlank String text) {}

    private final WhatsAppService whatsApp;
    private final MessageLogRepository logs;

    @Value("${whatsapp.verify-token}")
    private String verifyToken;

    public WhatsAppController(WhatsAppService whatsApp, MessageLogRepository logs) {
        this.whatsApp = whatsApp;
        this.logs = logs;
    }

    // ---------- Dashboard se use hone wale (login zaroori) ----------

    @GetMapping("/api/whatsapp/logs")
    public List<MessageLog> logs() {
        return logs.findTop100ByOrderByCreatedAtDesc();
    }

    @PostMapping("/api/whatsapp/send")
    public MessageLog send(@RequestBody SendRequest r) {
        return whatsApp.sendText(r.to(), r.text());
    }

    // ---------- Meta webhook (public, login nahi chahiye) ----------

    /** Meta webhook verify karte waqt yeh call karta hai. */
    @GetMapping("/webhook/whatsapp")
    public ResponseEntity<String> verify(
            @RequestParam(name = "hub.mode", required = false) String mode,
            @RequestParam(name = "hub.verify_token", required = false) String token,
            @RequestParam(name = "hub.challenge", required = false) String challenge) {
        if ("subscribe".equals(mode) && verifyToken.equals(token)) {
            return ResponseEntity.ok(challenge);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Verify token galat hai");
    }

    /** Customer ka incoming WhatsApp message yahan aata hai. */
    @PostMapping("/webhook/whatsapp")
    public ResponseEntity<String> receive(@RequestBody JsonNode payload) {
        for (JsonNode entry : payload.path("entry")) {
            for (JsonNode change : entry.path("changes")) {
                for (JsonNode msg : change.path("value").path("messages")) {
                    String from = msg.path("from").asText("");
                    String type = msg.path("type").asText("");
                    String body = "text".equals(type)
                            ? msg.path("text").path("body").asText("")
                            : "[" + type + " message]";
                    whatsApp.logIncoming(from, body);
                }
            }
        }
        // Meta ko hamesha 200 chahiye warna woh baar-baar retry karta hai
        return ResponseEntity.ok("OK");
    }
}
