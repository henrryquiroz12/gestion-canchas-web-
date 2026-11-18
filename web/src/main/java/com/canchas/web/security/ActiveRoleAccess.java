package com.canchas.web.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;

/** Uses assigned roles as the permission boundary and the session as the active context. */
public final class ActiveRoleAccess {
    public static final String SESSION_ATTRIBUTE = "ACTIVE_ROLE";

    private ActiveRoleAccess() {
    }

    public static boolean hasRole(Authentication authentication, String role) {
        return authentication != null && authentication.isAuthenticated()
                && authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + role));
    }

    public static String resolve(Authentication authentication, HttpServletRequest request) {
        var session = request == null ? null : request.getSession(false);
        Object selected = session == null ? null : session.getAttribute(SESSION_ATTRIBUTE);
        if (selected != null) {
            String role = selected.toString();
            return ("ADMIN".equals(role) || "CLIENTE".equals(role)) && hasRole(authentication, role)
                    ? role : null;
        }
        boolean admin = hasRole(authentication, "ADMIN");
        boolean client = hasRole(authentication, "CLIENTE");
        // A multi-role account must explicitly select its context.
        return admin == client ? null : admin ? "ADMIN" : "CLIENTE";
    }

    public static boolean canAccess(Authentication authentication, HttpServletRequest request, String role) {
        return role.equals(resolve(authentication, request));
    }
}
