package com.kamsarobar.user;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kamsarobar.security.UserPrincipal;
import com.kamsarobar.user.dto.ChangePasswordRequest;
import com.kamsarobar.user.dto.UpdateUserRequest;
import com.kamsarobar.user.dto.UserResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users/me")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public UserResponse me(@AuthenticationPrincipal UserPrincipal principal) {
        return userService.get(principal.id());
    }

    @PutMapping
    public UserResponse update(@AuthenticationPrincipal UserPrincipal principal,
                               @Valid @RequestBody UpdateUserRequest request) {
        return userService.updateBasicInfo(principal.id(), request);
    }

    @PutMapping("/password")
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal UserPrincipal principal,
                                               @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(principal.id(), request);
        return ResponseEntity.noContent().build();
    }
}
