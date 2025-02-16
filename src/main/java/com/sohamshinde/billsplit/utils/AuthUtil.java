package com.sohamshinde.billsplit.utils;

import com.sohamshinde.billsplit.entity.User;
import org.springframework.security.core.context.SecurityContextHolder;

public class AuthUtil {

    // Common method to get the authenticated user from the security context
    public static User getAuthenticatedUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
