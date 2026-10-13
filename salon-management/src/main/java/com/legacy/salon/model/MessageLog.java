package com.legacy.salon.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "message_logs")
public class MessageLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    /** IN = customer ne bheja, OUT = salon ne bheja */
    public String direction;

    public String phone;

    @Column(length = 4000)
    public String body;

    /** SENT, FAILED, NOT_CONFIGURED, RECEIVED */
    public String status;

    @Column(length = 2000)
    public String error;

    public LocalDateTime createdAt = LocalDateTime.now();
}
