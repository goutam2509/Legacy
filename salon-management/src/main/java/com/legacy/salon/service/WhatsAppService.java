package com.legacy.salon.service;

import com.legacy.salon.model.MessageLog;
import com.legacy.salon.repo.MessageLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * WhatsApp Cloud API (Meta) ke through message bhejta hai.
 * Token / phone number id .env mein daalo. Khali hone par message "NOT_CONFIGURED" log hota hai.
 */
@Service
public class WhatsAppService {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppService.class);

    private final MessageLogRepository logs;
    private final RestClient http = RestClient.create();

    @Value("${whatsapp.token:}")
    private String token;
    @Value("${whatsapp.phone-number-id:}")
    private String phoneNumberId;
    @Value("${whatsapp.api-version:v21.0}")
    private String apiVersion;
    @Value("${whatsapp.default-country-code:91}")
    private String defaultCountryCode;
    @Value("${whatsapp.confirm-template:}")
    private String confirmTemplate;
    @Value("${whatsapp.template-language:en}")
    private String templateLanguage;

    public WhatsAppService(MessageLogRepository logs) {
        this.logs = logs;
    }

    public boolean isConfigured() {
        return !token.isBlank() && !phoneNumberId.isBlank();
    }

    public String getConfirmTemplate() {
        return confirmTemplate;
    }

    /** "98765 43210" / "+91 98765-43210" / "09876543210" -> "919876543210" */
    public String normalizePhone(String raw) {
        String digits = raw == null ? "" : raw.replaceAll("\\D", "");
        if (digits.startsWith("0")) digits = digits.substring(1);
        if (digits.length() == 10) digits = defaultCountryCode + digits;
        return digits;
    }

    /** Normal text message (customer ne pichhle 24 ghante mein message kiya ho tabhi deliver hota hai). */
    public MessageLog sendText(String to, String text) {
        Map<String, Object> payload = Map.of(
                "messaging_product", "whatsapp",
                "to", normalizePhone(to),
                "type", "text",
                "text", Map.of("body", text));
        return send(to, text, payload);
    }

    /** Approved template message (business-initiated chat ke liye yahi chahiye). */
    public MessageLog sendTemplate(String to, String templateName, List<String> bodyParams) {
        Map<String, Object> template = new java.util.LinkedHashMap<>();
        template.put("name", templateName);
        template.put("language", Map.of("code", templateLanguage));
        if (bodyParams != null && !bodyParams.isEmpty()) {
            List<Map<String, String>> params = bodyParams.stream()
                    .map(p -> Map.of("type", "text", "text", p)).toList();
            template.put("components", List.of(Map.of("type", "body", "parameters", params)));
        }
        Map<String, Object> payload = Map.of(
                "messaging_product", "whatsapp",
                "to", normalizePhone(to),
                "type", "template",
                "template", template);
        return send(to, "[template: " + templateName + "] " + String.join(", ", bodyParams == null ? List.of() : bodyParams), payload);
    }

    private MessageLog send(String to, String bodyForLog, Map<String, Object> payload) {
        MessageLog entry = new MessageLog();
        entry.direction = "OUT";
        entry.phone = normalizePhone(to);
        entry.body = bodyForLog;

        if (!isConfigured()) {
            entry.status = "NOT_CONFIGURED";
            entry.error = ".env mein WHATSAPP_TOKEN aur WHATSAPP_PHONE_NUMBER_ID set karo";
            return logs.save(entry);
        }
        try {
            String url = "https://graph.facebook.com/" + apiVersion + "/" + phoneNumberId + "/messages";
            http.post().uri(url)
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
            entry.status = "SENT";
        } catch (Exception e) {
            log.warn("WhatsApp send failed: {}", e.getMessage());
            entry.status = "FAILED";
            String msg = e.getMessage();
            entry.error = msg != null && msg.length() > 1900 ? msg.substring(0, 1900) : msg;
        }
        return logs.save(entry);
    }

    public MessageLog logIncoming(String from, String body) {
        MessageLog entry = new MessageLog();
        entry.direction = "IN";
        entry.phone = from;
        entry.body = body;
        entry.status = "RECEIVED";
        return logs.save(entry);
    }
}
