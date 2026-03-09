package com.keybudget.category;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Represents a budget/transaction category. System defaults have {@code userId} null;
 * user-created categories carry the owning user's id.
 */
@Entity
@Table(name = "categories")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    /** Icon identifier understood by the frontend (e.g. "home", "car"). */
    private String icon;

    /** Hex color string (e.g. "#4CAF50"). */
    private String color;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CategoryType type;

    /** Null for system-default categories; non-null for user-created ones. */
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        this.createdAt = Instant.now();
    }

    // Getters

    public Long getId() { return id; }

    public String getName() { return name; }

    public String getIcon() { return icon; }

    public String getColor() { return color; }

    public CategoryType getType() { return type; }

    public Long getUserId() { return userId; }

    public Instant getCreatedAt() { return createdAt; }

    // Setters

    public void setName(String name) { this.name = name; }

    public void setIcon(String icon) { this.icon = icon; }

    public void setColor(String color) { this.color = color; }

    public void setType(CategoryType type) { this.type = type; }

    public void setUserId(Long userId) { this.userId = userId; }
}
