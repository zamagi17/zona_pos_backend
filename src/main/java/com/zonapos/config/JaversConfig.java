package com.zonapos.config;

import com.zonapos.entity.User;
import org.javers.spring.auditable.AuthorProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Configuration
public class JaversConfig {

    @Bean
    public AuthorProvider authorProvider() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return "SYSTEM";
            }
            Object principal = authentication.getPrincipal();
            if (principal instanceof User user) {
                return user.getEmail() + " (" + user.getName() + ")";
            }
            return authentication.getName();
        };
    }
}
