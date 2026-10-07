package com.kamsarobar.security;

import java.util.Optional;

/**
 * Abstraction over the token format so the rest of the app does not depend on JWT specifics.
 */
public interface TokenService {

    String issueToken(UserPrincipal principal);

    /** Returns the user id carried by a valid token, or empty if the token is invalid or expired. */
    Optional<Long> resolveUserId(String token);
}
