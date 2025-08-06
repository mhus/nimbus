package de.mhus.nimbus.identity.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;

/**
 * Entity representing a user in the identity system.
 * Contains user information including credentials and roles.
 */
@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    /**
     * Unique identifier and login name of the user
     */
    @Id
    @Column(length = 50)
    private String id;

    /**
     * Full name of the user
     */
    @Column(nullable = false, length = 200)
    private String name;

    /**
     * Nickname of the user
     */
    @Column(length = 100)
    private String nickname;

    /**
     * Email address of the user
     */
    @Column(nullable = false, unique = true, length = 200)
    private String email;

    /**
     * List of roles assigned to the user
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role")
    private List<String> roles;

    /**
     * Timestamp when the user was created (Unix time)
     */
    @Column(name = "created_at", nullable = false)
    private Long createdAt;

    /**
     * Timestamp when the user was last updated (Unix time)
     */
    @Column(name = "updated_at", nullable = false)
    private Long updatedAt;

    /**
     * SHA256 hash of the user's password (salt is the user ID)
     */
    @Column(name = "password_hash", nullable = false, length = 64)
    private String passwordHash;

    @PrePersist
    protected void onCreate() {
        long now = System.currentTimeMillis() / 1000;
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = System.currentTimeMillis() / 1000;
    }
}
