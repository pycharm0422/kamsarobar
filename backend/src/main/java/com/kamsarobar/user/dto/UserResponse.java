package com.kamsarobar.user.dto;

import java.time.LocalDateTime;

import com.kamsarobar.city.dto.CitySummary;
import com.kamsarobar.user.Role;
import com.kamsarobar.user.User;

public record UserResponse(Long id, String name, String mobile, Role role, CitySummary city,
                           CitySummary managedCity, LocalDateTime createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getMobile(), user.getRole(),
                CitySummary.from(user.getCity()), CitySummary.from(user.getManagedCity()), user.getCreatedAt());
    }
}
