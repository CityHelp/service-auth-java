package com.crudzaso.cityhelp.auth.infrastructure.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Map;

/**
 * Custom implementation of OAuth2User that wraps the standard OAuth2User
 * and adds our custom user information (userId, email).
 *
 * This class is used to carry user information through the authentication flow
 * and make it available to the OAuth2AuthenticationSuccessHandler.
 *
 * @author CityHelp Team
 */
public class CustomOAuth2User implements OAuth2User {

    private final OAuth2User oauth2User;
    private final Long userId;
    private final String email;

    public CustomOAuth2User(OAuth2User oauth2User, Long userId, String email) {
        this.oauth2User = oauth2User;
        this.userId = userId;
        this.email = email;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return oauth2User.getAttributes();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return oauth2User.getAuthorities();
    }

    @Override
    public String getName() {
        return oauth2User.getName();
    }

    /**
     * Get the user ID from our database.
     *
     * @return User ID
     */
    public Long getUserId() {
        return userId;
    }

    /**
     * Get the user email.
     *
     * @return User email
     */
    public String getEmail() {
        return email;
    }
}
