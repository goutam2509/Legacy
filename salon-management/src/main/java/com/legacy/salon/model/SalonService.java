package com.legacy.salon.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Entity
@Table(name = "salon_services")
public class SalonService {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @NotBlank(message = "Service ka naam zaroori hai")
    public String name;

    @NotNull(message = "Price zaroori hai")
    public BigDecimal price;

    @Min(value = 5, message = "Duration kam se kam 5 minute")
    public int durationMinutes = 30;

    public boolean active = true;
}
