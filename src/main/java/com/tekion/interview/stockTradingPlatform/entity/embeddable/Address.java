package com.tekion.interview.stockTradingPlatform.entity.embeddable;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Address {
    private String line1;
    private String line2;
    private String city;
    private String state;
    private String pincode;
    private String country;
}
