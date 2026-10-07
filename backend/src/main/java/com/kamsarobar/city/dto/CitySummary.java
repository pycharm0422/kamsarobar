package com.kamsarobar.city.dto;

import com.kamsarobar.city.City;

public record CitySummary(Long id, String name, String state, boolean active) {

    public static CitySummary from(City city) {
        return city == null ? null : new CitySummary(city.getId(), city.getName(), city.getState(), city.isActive());
    }
}
