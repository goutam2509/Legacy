package com.legacy.salon.web;

import com.legacy.salon.model.SalonService;
import com.legacy.salon.repo.AppointmentRepository;
import com.legacy.salon.repo.ServiceRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/services")
public class ServiceController {

    private final ServiceRepository services;
    private final AppointmentRepository appointments;

    public ServiceController(ServiceRepository services, AppointmentRepository appointments) {
        this.services = services;
        this.appointments = appointments;
    }

    @GetMapping
    public List<SalonService> list() {
        return services.findAllByOrderByNameAsc();
    }

    @PostMapping
    public SalonService create(@Valid @RequestBody SalonService s) {
        s.id = null;
        return services.save(s);
    }

    @PutMapping("/{id}")
    public SalonService update(@PathVariable Long id, @Valid @RequestBody SalonService in) {
        SalonService s = services.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Service nahi mili"));
        s.name = in.name;
        s.price = in.price;
        s.durationMinutes = in.durationMinutes;
        s.active = in.active;
        return services.save(s);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        if (appointments.countByServiceId(id) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Is service par appointments hain. Delete ki jagah usse inactive kar do");
        }
        services.deleteById(id);
    }
}
