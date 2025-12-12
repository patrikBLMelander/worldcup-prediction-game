package com.worldcup.security;

import com.worldcup.entity.Role;
import com.worldcup.entity.User;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class AdminAspect {

    private final CurrentUser currentUser;

    @Before("@annotation(com.worldcup.security.AdminRequired)")
    public void checkAdmin(JoinPoint joinPoint) {
        User user = currentUser.getCurrentUserOrThrow();
        if (user.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Admin access required");
        }
    }
}


