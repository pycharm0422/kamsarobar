package com.kamsarobar.access;

import java.util.Objects;

import org.springframework.stereotype.Component;

import com.kamsarobar.security.UserPrincipal;
import com.kamsarobar.user.Role;

@Component
public class RoleBasedAccessPolicy implements AccessPolicy {

    @Override
    public boolean canManageCity(UserPrincipal user, Long cityId) {
        if (user == null) {
            return false;
        }
        return user.isMainAdmin()
                || (user.role() == Role.CITY_ADMIN && Objects.equals(user.managedCityId(), cityId));
    }

    @Override
    public boolean canModifyContent(UserPrincipal user, Long authorId, Long cityId) {
        if (user == null) {
            return false;
        }
        return Objects.equals(user.id(), authorId) || canManageCity(user, cityId);
    }
}
