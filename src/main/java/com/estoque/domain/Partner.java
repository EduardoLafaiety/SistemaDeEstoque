package com.estoque.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Fornecedor e/ou cliente. */
@Entity
public class Partner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 150)
    @Column(nullable = false, length = 150)
    private String name;

    @Email
    @Size(max = 150)
    private String email;

    @Size(max = 40)
    private String phone;

    @Size(max = 30)
    private String document;

    private boolean supplier;

    private boolean customer;

    private boolean active = true;

    public Partner() {
    }

    public Partner(String name, boolean supplier, boolean customer) {
        this.name = name;
        this.supplier = supplier;
        this.customer = customer;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getDocument() { return document; }
    public void setDocument(String document) { this.document = document; }
    public boolean isSupplier() { return supplier; }
    public void setSupplier(boolean supplier) { this.supplier = supplier; }
    public boolean isCustomer() { return customer; }
    public void setCustomer(boolean customer) { this.customer = customer; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
