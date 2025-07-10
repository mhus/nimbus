package de.mhus.nimbus.identity.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * Access Control Entity (ACE) für das Identity System
 * Repräsentiert eine Berechtigung für einen Benutzer mit definierten Regeln und Reihenfolge
 */
@Entity
@Table(name = "ace",
       indexes = {
           @Index(name = "idx_ace_user_order", columnList = "user_id, order_value"),
           @Index(name = "idx_ace_rule", columnList = "rule")
       })
public class Ace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "rule", nullable = false, length = 500)
    private String rule;

    @NotNull
    @Column(name = "order_value", nullable = false)
    private Integer orderValue;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "is_active")
    private Boolean active = true;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

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
    public Ace() {}

    // Constructor
    public Ace(String rule, Integer orderValue, User user) {
        this.rule = rule;
        this.orderValue = orderValue;
        this.user = user;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRule() {
        return rule;
    }

    public void setRule(String rule) {
        this.rule = rule;
    }

    public Integer getOrderValue() {
        return orderValue;
    }

    public void setOrderValue(Integer orderValue) {
        this.orderValue = orderValue;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    @Override
    public String toString() {
        return "Ace{" +
                "id=" + id +
                ", rule='" + rule + '\'' +
                ", orderValue=" + orderValue +
                ", active=" + active +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Ace)) return false;
        Ace ace = (Ace) o;
        return id != null && id.equals(ace.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
