package com.tekion.interview.stockTradingPlatform.entity.embeddable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Deliberately NOT a JPA @Embeddable - if it were, its fields would map to plaintext columns.
 * Instead this whole object is serialized to JSON and passed through KycEncryptedConverter,
 * so PAN/Aadhaar never touch the DB in plaintext even if someone dumps the table.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KycDetails {
    private String panNumber;
    private String aadhaarLast4;
    private String kycStatus;
}
