package com.legacy.salon.web;

import com.legacy.salon.model.Appointment;
import com.legacy.salon.model.Customer;
import com.legacy.salon.model.SalonService;
import com.legacy.salon.repo.AppointmentRepository;
import com.legacy.salon.repo.CustomerRepository;
import com.legacy.salon.repo.ServiceRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Bina login ke customer ke liye: services dekho aur appointment book karo.
 * Booking seedha database (Supabase) mein save hoti hai aur dashboard par "Nayi online booking" dikhti hai.
 */
@RestController
@RequestMapping("/api/public")
public class PublicController {

    public record BookingRequest(
            @NotBlank(message = "Naam likho") @Size(max = 100, message = "Naam bahut lamba hai") String name,
            @NotBlank(message = "Phone number likho") String phone,
            @NotNull(message = "Service chuno") Long serviceId,
            @NotNull(message = "Date aur time chuno") LocalDateTime startTime,
            @Size(max = 500, message = "Note bahut lamba hai") String notes,
            String website) {}   // "website" = bot trap, asli user ise khali chhodta hai

    private final ServiceRepository services;
    private final CustomerRepository customers;
    private final AppointmentRepository appointments;

    public PublicController(ServiceRepository services, CustomerRepository customers,
                            AppointmentRepository appointments) {
        this.services = services;
        this.customers = customers;
        this.appointments = appointments;
    }

    @GetMapping("/services")
    public List<Map<String, Object>> services() {
        return services.findAllByOrderByNameAsc().stream()
                .filter(s -> s.active)
                .map(s -> Map.<String, Object>of(
                        "id", s.id,
                        "name", s.name,
                        "price", s.price,
                        "durationMinutes", s.durationMinutes))
                .toList();
    }

    @PostMapping("/book")
    public Map<String, Object> book(@Valid @RequestBody BookingRequest r) {
        // Bot ne trap field bhara hai: chupchap "ok" bol do, kuch save mat karo
        if (r.website() != null && !r.website().isBlank()) {
            return Map.of("ok", true);
        }

        String digits = r.phone().replaceAll("\\D", "");
        if (digits.length() < 10 || digits.length() > 13) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sahi phone number likho (10 digit)");
        }
        if (!r.startTime().isAfter(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Aage ka date aur time chuno");
        }
        SalonService service = services.findById(r.serviceId())
                .filter(s -> s.active)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ye service abhi available nahi hai"));

        // Isi phone number ka customer pehle se hai to wahi use karo, warna naya banao
        String last10 = digits.substring(digits.length() - 10);
        Customer customer = customers.findAll().stream()
                .filter(c -> c.phone != null && c.phone.replaceAll("\\D", "").endsWith(last10))
                .findFirst()
                .orElseGet(() -> {
                    Customer c = new Customer();
                    c.name = r.name().trim();
                    c.phone = last10;
                    return customers.save(c);
                });

        Appointment a = new Appointment();
        a.customer = customer;
        a.service = service;
        a.startTime = r.startTime();
        a.price = service.price;
        a.status = Appointment.Status.SCHEDULED;
        a.notes = r.notes();
        a.source = "ONLINE";
        a.seen = Boolean.FALSE;
        appointments.save(a);

        return Map.of("ok", true);
    }
}
