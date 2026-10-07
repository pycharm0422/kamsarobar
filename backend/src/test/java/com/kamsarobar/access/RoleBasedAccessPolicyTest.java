package com.kamsarobar.access;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.kamsarobar.security.UserPrincipal;
import com.kamsarobar.user.Role;

class RoleBasedAccessPolicyTest {

    private final AccessPolicy policy = new RoleBasedAccessPolicy();

    private final UserPrincipal member = new UserPrincipal(1L, "M", "91", "x", Role.MEMBER, 10L, null);
    private final UserPrincipal cityAdmin = new UserPrincipal(2L, "C", "92", "x", Role.CITY_ADMIN, 10L, 10L);
    private final UserPrincipal mainAdmin = new UserPrincipal(3L, "A", "93", "x", Role.MAIN_ADMIN, 10L, null);

    @Test
    void onlyAssignedCityAdminOrMainAdminManagesACity() {
        assertThat(policy.canManageCity(member, 10L)).isFalse();
        assertThat(policy.canManageCity(cityAdmin, 10L)).isTrue();
        assertThat(policy.canManageCity(cityAdmin, 11L)).isFalse();
        assertThat(policy.canManageCity(mainAdmin, 11L)).isTrue();
    }

    @Test
    void authorsAndCityAdminsCanModifyContent() {
        assertThat(policy.canModifyContent(member, 1L, 10L)).isTrue();
        assertThat(policy.canModifyContent(member, 99L, 10L)).isFalse();
        assertThat(policy.canModifyContent(cityAdmin, 99L, 10L)).isTrue();
        assertThat(policy.canModifyContent(cityAdmin, 99L, 11L)).isFalse();
    }

    @Test
    void blockingRules() {
        // main admin: anyone but themselves; never another main admin
        assertThat(policy.canBlockMember(mainAdmin, 1L, Role.MEMBER, 99L)).isTrue();
        assertThat(policy.canBlockMember(mainAdmin, 2L, Role.CITY_ADMIN, 10L)).isTrue();
        assertThat(policy.canBlockMember(mainAdmin, 3L, Role.MAIN_ADMIN, 10L)).isFalse();
        // city admin: ordinary members of their own city only
        assertThat(policy.canBlockMember(cityAdmin, 1L, Role.MEMBER, 10L)).isTrue();
        assertThat(policy.canBlockMember(cityAdmin, 1L, Role.MEMBER, 11L)).isFalse();
        assertThat(policy.canBlockMember(cityAdmin, 5L, Role.CITY_ADMIN, 10L)).isFalse();
        assertThat(policy.canBlockMember(cityAdmin, 2L, Role.MEMBER, 10L)).isFalse(); // themselves
        // members: nobody
        assertThat(policy.canBlockMember(member, 7L, Role.MEMBER, 10L)).isFalse();
    }
}
