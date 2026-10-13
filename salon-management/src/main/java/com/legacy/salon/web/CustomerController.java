package com.legacy.salon.web;

import com.legacy.salon.model.Customer;
import com.legacy.salon.repo.AppointmentRepository;
import com.legacy.salon.repo.CustomerRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerRepository customers;
    private final AppointmentRepository appointments;

    public CustomerController(CustomerRepository customers, AppointmentRepository appointments) {
        this.customers = customers;
        this.appointments = appointments;
    }

    @GetMapping
    public List<Customer> list() {
        return customers.findAllByOrderByNameAsc();
    }

    @PostMapping
    public Customer create(@Valid @RequestBody Customer c) {
        c.id = null;
        return customers.save(c);
    }

    @PutMapping("/{id}")
    public Customer update(@PathVariable Long id, @Valid @RequestBody Customer in) {
        Customer c = customers.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer nahi mila"));
        c.name = in.name;
        c.phone = in.phone;
        c.email = in.email;
        c.notes = in.notes;
        return customers.save(c);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        if (appointments.countByCustomerId(id) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Is customer ki appointments hain, pehle woh delete/cancel karo");
        }
        customers.deleteById(id);
    }
}
