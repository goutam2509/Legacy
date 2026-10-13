package com.legacy.salon.web;

import com.legacy.salon.model.Appointment;
import com.legacy.salon.model.Customer;
import com.legacy.salon.model.MessageLog;
import com.legacy.salon.model.SalonService;
import com.legacy.salon.repo.AppointmentRepository;
import com.legacy.salon.repo.CustomerRepository;
import com.legacy.salon.repo.ServiceRepository;
import com.legacy.salon.service.WhatsAppService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    public record AppointmentRequest(
            @NotNull(message = "Customer chuno") Long customerId,
            @NotNull(message = "Service chuno") Long serviceId,
            @NotNull(message = "Date/time chuno") LocalDateTime startTime,
            BigDecimal price,
            Appointment.Status status,
            String notes) {}

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    private final AppointmentRepository appointments;
    private final CustomerRepository customers;
    private final ServiceRepository services;
    private final WhatsAppService whatsApp;

    public AppointmentController(AppointmentRepository appointments, CustomerRepository customers,
                                 ServiceRepository services, WhatsAppService whatsApp) {
        this.appointments = appointments;
        this.customers = customers;
        this.services = services;
        this.whatsApp = whatsApp;
    }

    @GetMapping
    public List<Appointment> list() {
        return appointments.findAllByOrderByStartTimeDesc();
    }

    @PostMapping
    public Appointment create(@Valid @RequestBody AppointmentRequest r) {
        Appointment a = new Appointment();
        apply(a, r);
        return appointments.save(a);
    }

    @PutMapping("/{id}")
    public Appointment update(@PathVariable Long id, @Valid @RequestBody AppointmentRequest r) {
        Appointment a = find(id);
        apply(a, r);
        return appointments.save(a);
    }

    @PatchMapping("/{id}/status")
    public Appointment setStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Appointment a = find(id);
        try {
            a.status = Appointment.Status.valueOf(body.getOrDefault("status", "").toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status galat hai");
        }
        return appointments.save(a);
    }

    /** Admin ne nayi online booking dekh li: "NEW" hata do. */
    @PatchMapping("/{id}/seen")
    public Appointment markSeen(@PathVariable Long id) {
        Appointment a = find(id);
        a.seen = Boolean.TRUE;
        return appointments.save(a);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        appointments.deleteById(id);
    }

    /** Customer ko WhatsApp par appointment confirmation bhejo. */
    @PostMapping("/{id}/whatsapp")
    public MessageLog sendWhatsApp(@PathVariable Long id) {
        Appointment a = find(id);
        String when = a.startTime.format(FMT);
        MessageLog result;
        String template = whatsApp.getConfirmTemplate();
        if (template != null && !template.isBlank()) {
            // Template ke {{1}}, {{2}}, {{3}} = naam, service, samay
            result = whatsApp.sendTemplate(a.customer.phone, template,
                    List.of(a.customer.name, a.service.name, when));
        } else {
            String text = "Namaste " + a.customer.name + "! Aapki " + a.service.name
                    + " ki appointment " + when + " ko confirm hai. Dhanyavaad!";
            result = whatsApp.sendText(a.customer.phone, text);
        }
        if ("SENT".equals(result.status)) {
            a.whatsappSent = true;
            appointments.save(a);
        }
        return result;
    }

    private Appointment find(Long id) {
        return appointments.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment nahi mili"));
    }

    private void apply(Appointment a, AppointmentRequest r) {
        Customer c = customers.findById(r.customerId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Customer nahi mila"));
        SalonService s = services.findById(r.serviceId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Service nahi mili"));
        a.customer = c;
        a.service = s;
        a.startTime = r.startTime();
        a.price = r.price() != null ? r.price() : s.price;
        if (r.status() != null) a.status = r.status();
        a.notes = r.notes();
    }
}
