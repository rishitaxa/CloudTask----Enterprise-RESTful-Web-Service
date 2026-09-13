package com.cloudtask.export;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Resolves the internal owner/user ID from the current Authentication.
 *
 * This is a placeholder — wire it up to however CloudTask already
 * identifies the authenticated user (JWT claims, UserDetails, a custom
 * Principal, etc.). Keeping this as its own component means the export
 * controller and service don't need to know the details of your auth setup.
 */
@Component
public class AuthenticatedUserResolver {

    public Long resolveOwnerId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("No authenticated user in context");
        }

        // Example if using a custom UserDetails implementation:
        // if (authentication.getPrincipal() instanceof CloudTaskUserDetails userDetails) {
        //     return userDetails.getUserId();
        // }

        // Example if using JWT with a numeric "sub"/"uid" claim via Spring Security OAuth2:
        // if (authentication instanceof JwtAuthenticationToken jwtAuth) {
        //     return Long.valueOf(jwtAuth.getToken().getClaimAsString("uid"));
        // }

        throw new UnsupportedOperationException(
                "Wire AuthenticatedUserResolver.resolveOwnerId() up to CloudTask's actual "
                        + "Authentication/Principal implementation.");
    }
}
