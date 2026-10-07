package com.kamsarobar.security;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.kamsarobar.user.Role;
import com.kamsarobar.user.User;

/**
 * Immutable snapshot of the authenticated user, decoupled from the JPA entity.
 */
public record UserPrincipal(Long id, String name, String mobile, String passwordHash, Role role, Long cityId,
                            Long managedCityId) implements UserDetails {

    public static UserPrincipal from(User user) {
        return new UserPrincipal(user.getId(), user.getName(), user.getMobile(), user.getPasswordHash(),
                user.getRole(), user.getCity().getId(),
                user.getManagedCity() == null ? null : user.getManagedCity().getId());
    }

    public boolean isMainAdmin() {
        return role == Role.MAIN_ADMIN;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return mobile;
    }
}
