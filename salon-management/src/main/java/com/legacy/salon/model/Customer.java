package com.legacy.salon.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

@Entity
@Table(name = "customers")
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @NotBlank(message = "Naam zaroori hai")
    public String name;

    @NotBlank(message = "Phone number zaroori hai")
    public String phone;

    public String email;

    @Column(length = 1000)
    public String notes;

    public LocalDateTime createdAt = LocalDateTime.now();
}
