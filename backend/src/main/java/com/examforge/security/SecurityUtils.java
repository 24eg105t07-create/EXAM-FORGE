package com.examforge.security;

import com.examforge.entity.Role;
import com.examforge.exception.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {}

    public static UserPrincipal getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new UnauthorizedException("User is not authenticated");
        }
        return principal;
    }

    public static Long getCurrentUserId() {
        return getCurrentUser().getId();
    }

    public static Role getCurrentUserRole() {
        return getCurrentUser().getRole();
    }

    public static boolean isManagement() {
        return getCurrentUserRole() == Role.MANAGEMENT;
    }

    public static boolean isTeacher() {
        return getCurrentUserRole() == Role.TEACHER;
    }

    public static boolean isStudent() {
        return getCurrentUserRole() == Role.STUDENT;
    }
}
