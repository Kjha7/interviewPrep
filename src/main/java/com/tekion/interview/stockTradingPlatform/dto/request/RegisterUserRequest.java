package com.tekion.interview.stockTradingPlatform.dto.request;

import com.tekion.interview.stockTradingPlatform.entity.embeddable.Address;
import com.tekion.interview.stockTradingPlatform.entity.embeddable.ContactDetails;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class RegisterUserRequest {

    @NotBlank
    @Size(min = 4, max = 32)
    @Pattern(regexp = "^[a-zA-Z0-9_.]+$", message = "username may only contain letters, digits, '_' and '.'")
    private String username;

    @NotBlank
    private String name;

    @NotNull
    @Past
    private LocalDate dob;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String password;

    @NotNull
    private ContactDetails contactDetails;

    @NotNull
    private Address address;

    @NotNull
    private KycDetailsRequest kycDetails;

    @Data
    public static class KycDetailsRequest {
        @NotBlank
        @Pattern(regexp = "^[A-Z]{5}[0-9]{4}[A-Z]$", message = "invalid PAN format")
        private String panNumber;

        @NotBlank
        @Size(min = 4, max = 4)
        private String aadhaarLast4;
    }

    // NOTE: no "id" and no "age" fields here on purpose -
    // id is server-generated, age is derived from dob at read time, never stored/accepted as input.
}
