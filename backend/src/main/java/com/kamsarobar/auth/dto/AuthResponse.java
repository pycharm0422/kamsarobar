package com.kamsarobar.auth.dto;

import com.kamsarobar.user.dto.UserResponse;

public record AuthResponse(String token, UserResponse user) {
}
