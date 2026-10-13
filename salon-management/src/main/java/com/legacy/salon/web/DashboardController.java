package com.legacy.salon.web;

import com.legacy.salon.model.Appointment;
import com.legacy.salon.repo.AppointmentRepository;
import com.legacy.salon.repo.CustomerRepository;
import com.legacy.salon.repo.ServiceRepository;
import com.legacy.salon.service.WhatsAppService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class DashboardController {

    private final CustomerRepository customers;
    private final ServiceRepository services;
    private final AppointmentRepository appointments;
    private final WhatsAppService whatsApp;

    public DashboardController(CustomerRepository customers, ServiceRepository services,
                               AppointmentRepository appointments, WhatsAppService whatsApp) {
        this.customers = customers;
        this.services = services;
        this.appointments = appointments;
        this.whatsApp = whatsApp;
    }

    @GetMapping("/me")
    public Map<String, Object> me(Authentication auth) {
        return Map.of("username", auth.getName());
    }

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime startOfNextMonth = startOfMonth.plusMonths(1);

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("customers", customers.count());
        m.put("services", services.count());
        m.put("appointmentsToday", appointments.countByStartTimeBetweenAndStatusNot(
                startOfDay, endOfDay, Appointment.Status.CANCELLED));
        m.put("revenueThisMonth", appointments.revenueBetween(
                Appointment.Status.COMPLETED, startOfMonth, startOfNextMonth));
        m.put("todayList", appointments.findByStartTimeBetweenAndStatusNotOrderByStartTimeAsc(
                startOfDay, endOfDay, Appointment.Status.CANCELLED));
        m.put("newOnline", appointments.countBySourceAndSeen("ONLINE", Boolean.FALSE));
        m.put("newOnlineList", appointments.findBySourceAndSeenOrderByStartTimeAsc("ONLINE", Boolean.FALSE));
        m.put("whatsappConfigured", whatsApp.isConfigured());
        return m;
    }
}
