package com.estoque.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.estoque.domain.Partner;

public interface PartnerRepository extends JpaRepository<Partner, Long> {

    List<Partner> findBySupplierTrueAndActiveTrueOrderByName();

    List<Partner> findByActiveTrueOrderByName();

    @Query("""
            select p from Partner p
            where (:term = '' or lower(p.name) like lower(concat('%', :term, '%'))
                   or lower(coalesce(p.email, '')) like lower(concat('%', :term, '%'))
                   or coalesce(p.document, '') like concat('%', :term, '%'))
            """)
    Page<Partner> search(@Param("term") String term, Pageable pageable);
}
