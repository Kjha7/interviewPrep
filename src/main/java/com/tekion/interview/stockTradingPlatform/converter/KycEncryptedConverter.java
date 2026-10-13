package com.tekion.interview.stockTradingPlatform.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tekion.interview.stockTradingPlatform.entity.embeddable.KycDetails;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Field-level AES-GCM encryption for KYC data (PAN, Aadhaar).
 *
 * IMPORTANT - this is boilerplate, not production-ready key management:
 * the key here comes straight from application.properties. In production this key
 * should be a Data Encryption Key (DEK) fetched from a KMS (AWS KMS / GCP KMS / Vault),
 * itself encrypted at rest by a Key Encryption Key (KEK), with rotation support.
 * Do not ship this converter as-is with a static properties-file key.
 */
@Converter
@Component
public class KycEncryptedConverter implements AttributeConverter<KycDetails, String> {

    private static final String ALGO = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int IV_LENGTH_BYTES = 12;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SecretKeySpec keySpec;

    public KycEncryptedConverter(@Value("${app.security.kyc-encryption-key}") String base64Key) {
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        this.keySpec = new SecretKeySpec(keyBytes, "AES");
    }

    @Override
    @SneakyThrows
    public String convertToDatabaseColumn(KycDetails attribute) {
        if (attribute == null) return null;
        byte[] plaintext = objectMapper.writeValueAsBytes(attribute);

        byte[] iv = new byte[IV_LENGTH_BYTES];
        new SecureRandom().nextBytes(iv);

        Cipher cipher = Cipher.getInstance(ALGO);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
        byte[] ciphertext = cipher.doFinal(plaintext);

        // store iv || ciphertext, base64-encoded
        byte[] combined = new byte[iv.length + ciphertext.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);
        return Base64.getEncoder().encodeToString(combined);
    }

    @Override
    @SneakyThrows
    public KycDetails convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        byte[] combined = Base64.getDecoder().decode(dbData);

        byte[] iv = new byte[IV_LENGTH_BYTES];
        byte[] ciphertext = new byte[combined.length - IV_LENGTH_BYTES];
        System.arraycopy(combined, 0, iv, 0, IV_LENGTH_BYTES);
        System.arraycopy(combined, IV_LENGTH_BYTES, ciphertext, 0, ciphertext.length);

        Cipher cipher = Cipher.getInstance(ALGO);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
        byte[] plaintext = cipher.doFinal(ciphertext);

        return objectMapper.readValue(plaintext, KycDetails.class);
    }
}
