package de.mhus.nimbus.universe.user;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import de.mhus.nimbus.shared.security.HashService;
import de.mhus.nimbus.shared.user.UniverseRoles; // neu

@Service
@Validated
public class UUserService {

    private final UUserRepository userRepository;
    private final HashService hashService;

    public UUserService(UUserRepository userRepository, HashService hashService) {
        this.userRepository = userRepository;
        this.hashService = hashService;
    }

    public UUser createUser(String username, String email) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists: " + username);
        }
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists: " + email);
        }
        UUser user = new UUser(username, email);
        user.setRoles(UniverseRoles.USER); // Default Rolle
        return userRepository.save(user);
    }

    public Optional<UUser> getById(String id) {
        return userRepository.findById(id);
    }

    public Optional<UUser> getByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public List<UUser> listAll() {
        return userRepository.findAll();
    }

    public void deleteById(String id) {
        userRepository.deleteById(id);
    }

    /**
     * Update username, email and roles (raw comma-separated) of a user.
     * Performs uniqueness checks for username/email when changed.
     */
    public UUser update(String id, String username, String email, String rolesRaw) {
        UUser user = userRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));

        // Username
        if (username != null && !username.isBlank() && !username.equals(user.getUsername())) {
            if (userRepository.existsByUsername(username)) {
                throw new IllegalArgumentException("Username already exists: " + username);
            }
            user.setUsername(username);
        }

        // Email
        if (email != null && !email.isBlank() && !email.equals(user.getEmail())) {
            if (userRepository.existsByEmail(email)) {
                throw new IllegalArgumentException("Email already exists: " + email);
            }
            user.setEmail(email);
        }

        // Roles raw (may be null/blank to clear)
        if (rolesRaw != null) {
            user.setRolesStringList(rolesRaw);
        }

        return userRepository.save(user);
    }

    /**
     * Hashes and sets a new password for the user using the user's id as salt.
     * @param userId the user id
     * @param plainPassword raw password (must not be blank)
     * @return updated user
     */
    public UUser setPassword(String userId, String plainPassword) {
        if (plainPassword == null || plainPassword.isBlank()) {
            throw new IllegalArgumentException("Password must not be blank");
        }
        UUser user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        String hash = hashService.hash(plainPassword, user.getId()); // salt = userId
        user.setPasswordHash(hash);
        return userRepository.save(user);
    }

    /**
     * Validates a raw password against the stored hash.
     * @param userId user identifier
     * @param plainPassword raw password
     * @return true if valid
     */
    public boolean validatePassword(String userId, String plainPassword) {
        UUser user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        String hash = user.getPasswordHash();
        if (hash == null || hash.isBlank()) return false;
        // Support both salted and unsalted stored hashes transparently
        return validatePasswordHash(user.getId(), plainPassword, hash);
    }

    /**
     * Validates a plain password directly against a provided hash string.
     * Supports unsalted format algorithm;hashBase64 and salted algorithm:saltBase64:hashBase64.
     * If salted, the caller should provide the userId as salt for consistency.
     * @param plainPassword raw password
     * @param hash stored hash string (may contain salt)
     * @return true if matches
     */
    public boolean validatePasswordHash(String plainPassword, String hash) {
        if (plainPassword == null || hash == null) return false;
        // Decide salted vs unsalted by number of ':' parts
        int parts = hash.split(";").length;
        if (parts == 2) { // algorithm:hash
            return hashService.validate(plainPassword, hash);
        } else if (parts == 3) { // algorithm:salt:hash
            // Extract salt for validation path by re-parsing via HashService; HashService.validate requires clear salt
            String[] arr = hash.split(";",3);
            String saltBase64 = arr[1];
            String salt = new String(java.util.Base64.getDecoder().decode(saltBase64), java.nio.charset.StandardCharsets.UTF_8);
            return hashService.validate(plainPassword, salt, hash);
        } else {
            throw new IllegalArgumentException("Invalid hash format");
        }
    }

    /**
     * Validates a plain password against a salted hash using provided userId as salt reference.
     * Falls back to unsalted validate if hash has no salt portion.
     * @param userId expected salt (user id)
     * @param plainPassword raw password
     * @param hash hash string
     * @return true if valid and salt matches when present
     */
    public boolean validatePasswordHash(String userId, String plainPassword, String hash) {
        if (plainPassword == null || hash == null || userId == null) return false;
        int parts = hash.split(";").length;
        if (parts == 2) {
            return hashService.validate(plainPassword, hash);
        } else if (parts == 3) {
            String[] arr = hash.split(";",3);
            String saltBase64 = arr[1];
            String salt = new String(java.util.Base64.getDecoder().decode(saltBase64), java.nio.charset.StandardCharsets.UTF_8);
            if (!userId.equals(salt)) return false;
            return hashService.validate(plainPassword, salt, hash);
        } else {
            throw new IllegalArgumentException("Invalid hash format");
        }
    }
}
