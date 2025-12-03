package com.crudzaso.cityhelp.auth.infrastructure.security;

import com.crudzaso.cityhelp.auth.domain.model.User;
import com.crudzaso.cityhelp.auth.domain.repository.UserRepository;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

/**
 * Custom UserDetailsService implementation for Spring Security.
 * Loads user from database and converts to Spring Security UserDetails.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String emailOrUsername) throws UsernameNotFoundException {
        // Try to find user by email first
        User user = userRepository.findByEmailIgnoreCase(emailOrUsername)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Usuario no encontrado con email: " + emailOrUsername));

        // Check if user can login
        if (!user.canLogin()) {
            if (user.needsEmailVerification()) {
                throw new UsernameNotFoundException(
                        "Usuario pendiente de verificación de email: " + emailOrUsername);
            }
            throw new UsernameNotFoundException(
                    "Cuenta de usuario no activa: " + emailOrUsername);
        }

        // Handle null passwords for OAuth users by using empty string
        String password = user.getPassword() != null ? user.getPassword() : "";

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail()) // Use email as username
                .password(password) // Handle null password for OAuth users
                .authorities(Collections.singletonList(
                        new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
                ))
                .accountExpired(false)
                .accountLocked(user.getStatus().name().equals("SUSPENDED"))
                .credentialsExpired(false)
                .disabled(!user.getIsVerified())
                .build();
    }
}