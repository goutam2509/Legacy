package com.legacy.salon.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "appointments")
public class Appointment {
    public enum Status { SCHEDULED, COMPLETED, CANCELLED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    public Customer customer;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    public SalonService service;

    @Column(nullable = false)
    public LocalDateTime startTime;

    public BigDecimal price;

    @Enumerated(EnumType.STRING)
    public Status status = Status.SCHEDULED;

    @Column(length = 1000)
    public String notes;

    public boolean whatsappSent = false;

    /** ADMIN = dashboard se banayi, ONLINE = customer ne booking page se ki. (null = purani row = ADMIN) */
    public String source = "ADMIN";

    /** Online booking par false jab tak admin "Dekh liya" na dabaye. (null = purani row = seen) */
    public Boolean seen = Boolean.TRUE;
}
