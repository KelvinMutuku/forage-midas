package com.jpmc.midascore.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private BigDecimal balance;

    @OneToMany(mappedBy = "sender")
    private List<TransactionRecord> sentTransactions;

    @OneToMany(mappedBy = "recipient")
    private List<TransactionRecord> receivedTransactions;

    // Constructors, Getters, and Setters
}