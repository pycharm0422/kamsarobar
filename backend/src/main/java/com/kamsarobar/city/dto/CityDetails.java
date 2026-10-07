package com.kamsarobar.city.dto;

import com.kamsarobar.city.City;

/**
 * Full city information shown to signed-in members: WhatsApp group link and bank details for donations.
 */
public record CityDetails(Long id, String name, String state, boolean active, String whatsappGroupUrl,
                          BankDetails bank) {

    public record BankDetails(String accountName, String accountNumber, String ifsc, String bankName,
                              String upiId) {
    }

    public static CityDetails from(City city) {
        return new CityDetails(city.getId(), city.getName(), city.getState(), city.isActive(),
                city.getWhatsappGroupUrl(),
                new BankDetails(city.getBankAccountName(), city.getBankAccountNumber(), city.getBankIfsc(),
                        city.getBankName(), city.getUpiId()));
    }
}
