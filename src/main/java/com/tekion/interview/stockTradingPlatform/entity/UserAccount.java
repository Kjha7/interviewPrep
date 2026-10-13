package com.tekion.interview.stockTradingPlatform.entity;

import com.tekion.interview.stockTradingPlatform.converter.KycEncryptedConverter;
import com.tekion.interview.stockTradingPlatform.entity.embeddable.Address;
import com.tekion.interview.stockTradingPlatform.entity.embeddable.ContactDetails;
import com.tekion.interview.stockTradingPlatform.entity.embeddable.KycDetails;
import com.tekion.interview.stockTradingPlatform.entity.enums.UserStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "user_account", indexes = {
        @Index(name = "idx_user_account_username", columnList = "username", unique = true),
        @Index(name = "idx_user_account_email", columnList = "email", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 32)
    private String username;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private LocalDate dob;

    @Column(nullable = false, unique = true)
    private String email;

    @Embedded
    private Address address;

    @Embedded
    private ContactDetails contactDetails;

    // Whole KycDetails object is serialized + AES-GCM encrypted before hitting this column.
    @Convert(converter = KycEncryptedConverter.class)
    @Column(name = "kyc_info", columnDefinition = "TEXT")
    private KycDetails kycInfo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private UserStatus status;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant updatedAt;

    // Optimistic lock - protects concurrent profile edits, not the trading hot path.
    @Version
    private long version;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
        if (this.status == null) {
            this.status = UserStatus.PENDING_KYC_VERIFICATION;
        }
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
