package com.worldcup.service;

import com.worldcup.entity.User;
import com.worldcup.exception.EmailAlreadyExistsException;
import com.worldcup.exception.InvalidPasswordException;
import com.worldcup.exception.UserNotFoundException;
import com.worldcup.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public User createUser(String email, String password) {
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException(email);
        }

        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setEnabled(true);

        return userRepository.save(user);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public void changePassword(User user, String currentPassword, String newPassword) {
        // Verify current password
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new InvalidPasswordException("Current password is incorrect");
        }

        // Update password
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    /**
     * Admin-initiated password reset. Generates a random temporary password,
     * stores its hash, and returns the plaintext so the admin can hand it to
     * the user. The user is expected to change it after logging in.
     */
    public String adminResetPassword(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        String temporaryPassword = generateTemporaryPassword();
        user.setPassword(passwordEncoder.encode(temporaryPassword));
        userRepository.save(user);
        return temporaryPassword;
    }

    // Avoids ambiguous characters (0/O, 1/l/I) so temp passwords are easy to read aloud.
    private static final char[] TEMP_PASSWORD_ALPHABET =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789".toCharArray();
    private static final int TEMP_PASSWORD_LENGTH = 12;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private String generateTemporaryPassword() {
        StringBuilder sb = new StringBuilder(TEMP_PASSWORD_LENGTH);
        for (int i = 0; i < TEMP_PASSWORD_LENGTH; i++) {
            sb.append(TEMP_PASSWORD_ALPHABET[SECURE_RANDOM.nextInt(TEMP_PASSWORD_ALPHABET.length)]);
        }
        return sb.toString();
    }
}

