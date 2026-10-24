package com.estoque.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.estoque.domain.ReorderRule;

public interface ReorderRuleRepository extends JpaRepository<ReorderRule, Long> {

    @Query("""
            select r from ReorderRule r join fetch r.product p join fetch r.location l
            where r.active = true and p.active = true
            order by p.name, l.fullName
            """)
    List<ReorderRule> findAllActive();

    Optional<ReorderRule> findByProductIdAndLocationId(Long productId, Long locationId);

    List<ReorderRule> findByProductIdOrderById(Long productId);
}
