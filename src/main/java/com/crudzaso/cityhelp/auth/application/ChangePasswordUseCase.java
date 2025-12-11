package com.crudzaso.cityhelp.auth.application;

import com.crudzaso.cityhelp.auth.domain.model.User;
import com.crudzaso.cityhelp.auth.domain.repository.UserRepository;
import com.crudzaso.cityhelp.auth.application.exception.InvalidCredentialsException;
import com.crudzaso.cityhelp.auth.application.exception.UserNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class ChangePasswordUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public ChangePasswordUseCase(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Change user password after verifying current password.
     *
     * @param userId User ID
     * @param currentPassword Current password
     * @param newPassword New password
     * @throws UserNotFoundException if user not found
     * @throws InvalidCredentialsException if current password is wrong
     */
    public void execute(Long userId, String currentPassword, String newPassword) {
        // Find user
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new UserNotFoundException("Usuario no encontrado");
        }

        User user = userOpt.get();

        // Verify current password
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new InvalidCredentialsException("La contraseña actual es incorrecta");
        }

        // Update to new password
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.update(user);
    }
}
