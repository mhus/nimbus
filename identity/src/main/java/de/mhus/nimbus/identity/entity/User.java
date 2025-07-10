package de.mhus.nimbus.identity.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

/**
 * User Entity für das Identity System
 */
@Entity
@Table(name = "users")
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(unique = true)
    private String username;

    @NotBlank
    @Email
    @Column(unique = true)
    private String email;

    @NotBlank
    private String password;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "is_active")
    private Boolean active = true;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    // One-to-Many Beziehung zu IdentityCharacter
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<IdentityCharacter> identityCharacters = new ArrayList<>();

    // One-to-Many Beziehung zu ACE
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Ace> aces = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    // Default constructor
    public User() {}

    // Constructor
    public User(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
    }

    // UserDetails implementation
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.emptyList();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<IdentityCharacter> getIdentityCharacters() {
        return identityCharacters;
    }

    public void setIdentityCharacters(List<IdentityCharacter> identityCharacters) {
        this.identityCharacters = identityCharacters;
    }

    public List<Ace> getAces() {
        return aces;
    }

    public void setAces(List<Ace> aces) {
        this.aces = aces;
    }

    /**
     * Fügt einen IdentityCharacter zu diesem User hinzu
     */
    public void addIdentityCharacter(IdentityCharacter identityCharacter) {
        identityCharacters.add(identityCharacter);
        identityCharacter.setUser(this);
    }

    /**
     * Entfernt einen IdentityCharacter von diesem User
     */
    public void removeIdentityCharacter(IdentityCharacter identityCharacter) {
        identityCharacters.remove(identityCharacter);
        identityCharacter.setUser(null);
    }

    /**
     * Fügt einen ACE zu diesem User hinzu
     */
    public void addAce(Ace ace) {
        aces.add(ace);
        ace.setUser(this);
    }

    /**
     * Entfernt einen ACE von diesem User
     */
    public void removeAce(Ace ace) {
        aces.remove(ace);
        ace.setUser(null);
    }
}
