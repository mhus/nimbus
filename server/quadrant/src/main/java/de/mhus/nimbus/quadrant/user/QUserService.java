package de.mhus.nimbus.quadrant.user;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import de.mhus.nimbus.shared.user.UniverseRoles;

@Service
@Validated
public class QUserService {

    private final QUserRepository repository;

    public QUserService(QUserRepository repository) {
        this.repository = repository;
    }

    public QUser createUser(String username, String email) {
        if (username == null || username.isBlank()) throw new IllegalArgumentException("username is blank");
        if (email == null || email.isBlank()) throw new IllegalArgumentException("email is blank");
        if (repository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists: " + username);
        }
        if (repository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists: " + email);
        }
        QUser user = new QUser(username, email);
        user.setRoles(UniverseRoles.USER);
        return repository.save(user);
    }

    public Optional<QUser> getById(String id) {
        return repository.findById(id);
    }

    public Optional<QUser> getByUsername(String username) {
        return repository.findByUsername(username);
    }

    public List<QUser> listAll() {
        return repository.findAll();
    }

    public QUser update(String id, String username, String email, String rolesRaw) {
        QUser existing = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
        if (username != null && !username.equals(existing.getUsername())) {
            if (repository.existsByUsername(username)) throw new IllegalArgumentException("Username already exists: " + username);
            existing.setUsername(username);
        }
        if (email != null && !email.equals(existing.getEmail())) {
            if (repository.existsByEmail(email)) throw new IllegalArgumentException("Email already exists: " + email);
            existing.setEmail(email);
        }
        if (rolesRaw != null) {
            existing.setRolesRaw(rolesRaw);
        }
        return repository.save(existing);
    }

    public void deleteById(String id) {
        repository.deleteById(id);
    }

    public QUser addRole(String id, UniverseRoles role) {
        QUser existing = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
        if (existing.addRole(role)) {
            existing = repository.save(existing);
        }
        return existing;
    }

    public QUser removeRole(String id, UniverseRoles role) {
        QUser existing = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
        if (existing.removeRole(role)) {
            existing = repository.save(existing);
        }
        return existing;
    }
}
