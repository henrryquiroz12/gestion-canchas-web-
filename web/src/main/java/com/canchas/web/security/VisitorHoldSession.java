package com.canchas.web.security;

import jakarta.servlet.http.HttpSession;
import java.util.UUID;

public final class VisitorHoldSession {
    private VisitorHoldSession() {}
    public static final String OWNER = "VISITOR_HOLD_OWNER";
    public static final String FLOW = "VISITOR_HOLD_FLOW";
    public static String existingOwner(HttpSession session) {
        return session == null ? null : (String) session.getAttribute(OWNER);
    }
    public static String owner(HttpSession session) {
        synchronized (session) {
            if (existingOwner(session) == null) session.setAttribute(OWNER, UUID.randomUUID().toString());
            return existingOwner(session);
        }
    }
    public static boolean hasFlow(HttpSession session) {
        return session != null && Boolean.TRUE.equals(session.getAttribute(FLOW));
    }
}
