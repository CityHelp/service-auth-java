package com.crudzaso.cityhelp.auth.infrastructure.repository;

import com.crudzaso.cityhelp.auth.domain.model.RefreshToken;
import com.crudzaso.cityhelp.auth.domain.repository.RefreshTokenRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * JPA implementation of RefreshTokenRepository interface.
 * Follows English naming convention for technical code.
 * Implements all CRUD operations defined in the interface.
 * 
 * This class belongs to Infrastructure Layer as it contains framework dependencies.
 */
@Repository
public class RefreshTokenRepositoryImpl implements RefreshTokenRepository {
    
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    
    public RefreshTokenRepositoryImpl(
            RefreshTokenRepository refreshTokenRepository,
            UserRepository userRepository
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
    }
    
    @Override
    @Transactional
    public RefreshToken save(RefreshToken refreshToken) {
        // Set user reference
        refreshToken.setUser(refreshToken.getUser());
        
        // Generate new token with 30 days expiration
        refreshToken.setToken(generateRefreshToken());
        refreshToken.setExpiresAt(LocalDateTime.now().plusDays(30));
        refreshToken.setCreatedAt(LocalDateTime.now());
        refreshToken.setRevoked(false);
        
        // Save to repository
        RefreshToken savedToken = refreshTokenRepository.save(refreshToken);
        
        return savedToken;
    }
    
    @Override
    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }
    
    @Override
    public Optional<RefreshToken> findByUserId(Long userId) {
        return refreshTokenRepository.findByUserId(userId);
    }
    
    @Override
    public List<RefreshToken> findByUserId(Long userId) {
        return refreshTokenRepository.findByUserId(userId);
    }
    
    @Override
    public List<RefreshToken> findActiveByUserId(Long userId) {
        return refreshTokenRepository.findActiveByUserId(userId);
    }
    
    @Override
    public List<RefreshToken> findExpired() {
        return refreshTokenRepository.findExpired();
    }
    
    @Override
    public List<RefreshToken> findRevoked() {
        return refreshTokenRepository.findRevoked();
    }
    
    @Override
    public void revokeAllByUserId(Long userId) {
        List<RefreshToken> userTokens = findByUserId(userId);
        
        for (RefreshToken token : userTokens) {
            token.setRevoked(true);
        }
        
        refreshTokenRepository.saveAll(userTokens);
    }
    
    @Override
    public void deleteExpired() {
        refreshTokenRepository.deleteExpired();
    }
    
    @Override
    public void deleteRevoked() {
        refreshTokenRepository.deleteRevoked();
    }
    
    @Override
    public int deleteExpired() {
        return refreshTokenRepository.deleteExpired();
    }
    
    @Override
    public int deleteAllByUserId(Long userId) {
        return refreshTokenRepository.deleteAllByUserId(userId);
    }
    
    @Override
    public int deleteRevoked() {
        return refreshTokenRepository.deleteRevoked();
    }
    
    /**
     * Generate a new random refresh token.
     * 
     * @return 6-character alphanumeric string
     */
    private String generateRefreshToken() {
        return String.valueOf((int) (Math.random() * 1000000))
                .substring(0, 6);
    }
}