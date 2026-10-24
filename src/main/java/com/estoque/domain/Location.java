package com.estoque.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

/**
 * Local de estoque organizado em árvore (ex.: WH/Estoque/Prateleira A).
 * O caminho completo é mantido em {@code fullName} para consultas por subárvore.
 */
@Entity
public class Location {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 500)
    private String fullName;

    @ManyToOne(fetch = FetchType.LAZY)
    private Location parent;

    @Enumerated(EnumType.STRING)
    @Column(name = "location_usage", nullable = false, length = 20)
    private LocationUsage usage;

    private boolean active = true;

    public boolean isInternal() {
        return usage == LocationUsage.INTERNAL;
    }

    /** Verdadeiro se este local é {@code other} ou está dentro dele. */
    public boolean isChildOf(Location other) {
        return fullName.equals(other.fullName) || fullName.startsWith(other.fullName + "/");
    }

    public int getDepth() {
        return (int) fullName.chars().filter(c -> c == '/').count();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public Location getParent() { return parent; }
    public void setParent(Location parent) { this.parent = parent; }
    public LocationUsage getUsage() { return usage; }
    public void setUsage(LocationUsage usage) { this.usage = usage; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
