package com.estoque.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.estoque.domain.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByActiveTrueOrderByName();

    boolean existsBySkuIgnoreCase(String sku);

    boolean existsBySkuIgnoreCaseAndIdNot(String sku, Long id);

    /** {@code categoryId = 0} significa "todas as categorias". */
    @Query("""
            select p from Product p left join p.category c
            where p.active = :active
              and (:categoryId = 0 or c.id = :categoryId)
              and (:term = '' or lower(p.name) like lower(concat('%', :term, '%'))
                   or lower(coalesce(p.sku, '')) like lower(concat('%', :term, '%'))
                   or coalesce(p.barcode, '') = :term)
            """)
    Page<Product> search(@Param("term") String term, @Param("categoryId") long categoryId,
                         @Param("active") boolean active, Pageable pageable);
}
