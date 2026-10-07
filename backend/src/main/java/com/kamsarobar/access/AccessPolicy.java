package com.kamsarobar.access;

import com.kamsarobar.security.UserPrincipal;
import com.kamsarobar.user.Role;

/**
 * Central place for "who may do what" rules. Services depend on this abstraction rather than
 * re-implementing role checks, so changing a rule happens in exactly one class.
 */
public interface AccessPolicy {

    /** Main admin manages every city; a city admin manages only the city assigned to them. */
    boolean canManageCity(UserPrincipal user, Long cityId);

    /**
     * Who may block / unblock a member: never the main admin or yourself; the main admin may block anyone else;
     * a city admin may block ordinary members of the city they manage.
     */
    boolean canBlockMember(UserPrincipal actor, Long memberId, Role memberRole, Long memberCityId);

    /** Authors may change their own content; admins of the content's city may moderate it. */
    boolean canModifyContent(UserPrincipal user, Long authorId, Long cityId);

    default void requireCityManager(UserPrincipal user, Long cityId) {
        if (!canManageCity(user, cityId)) {
            throw new com.kamsarobar.common.exception.ForbiddenException("Only the admin of this city can do this");
        }
    }

    default void requireContentModifier(UserPrincipal user, Long authorId, Long cityId) {
        if (!canModifyContent(user, authorId, cityId)) {
            throw new com.kamsarobar.common.exception.ForbiddenException("You can only change your own content");
        }
    }
}
