package com.estoque.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

@Entity
public class Warehouse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 10)
    private String code;

    @ManyToOne(optional = false)
    private Location viewLocation;

    @ManyToOne(optional = false)
    private Location stockLocation;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public Location getViewLocation() { return viewLocation; }
    public void setViewLocation(Location viewLocation) { this.viewLocation = viewLocation; }
    public Location getStockLocation() { return stockLocation; }
    public void setStockLocation(Location stockLocation) { this.stockLocation = stockLocation; }
}
