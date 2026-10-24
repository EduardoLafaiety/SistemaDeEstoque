package com.estoque.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.estoque.domain.Warehouse;

public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {

    List<Warehouse> findAllByOrderById();

    boolean existsByCodeIgnoreCase(String code);
}
