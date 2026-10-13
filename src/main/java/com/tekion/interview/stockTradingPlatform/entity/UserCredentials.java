package com.tekion.interview.stockTradingPlatform.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_credentials")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCredentials {

    @Id
    private UUID userId; // shares PK with UserAccount.id (1:1)

    @Column(nullable = false)
    private String passwordHash; // bcrypt/argon2 - never plaintext, never reversible

    private int failedLoginAttempts;

    private Instant lockedUntil;

    private Instant updatedAt;

    @PreUpdate
    @PrePersist
    void touch() {
        this.updatedAt = Instant.now();
    }
}
