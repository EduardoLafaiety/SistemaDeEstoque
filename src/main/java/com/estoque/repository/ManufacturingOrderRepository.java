package com.estoque.repository;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.estoque.domain.ManufacturingOrder;
import com.estoque.domain.ProductionState;

public interface ManufacturingOrderRepository extends JpaRepository<ManufacturingOrder, Long> {

    @Query("""
            select o from ManufacturingOrder o join o.product p
            where o.state in :states
              and (:term = '' or lower(o.reference) like lower(concat('%', :term, '%'))
                   or lower(p.name) like lower(concat('%', :term, '%'))
                   or lower(coalesce(p.sku, '')) like lower(concat('%', :term, '%')))
            """)
    Page<ManufacturingOrder> search(@Param("states") List<ProductionState> states, @Param("term") String term,
                                    Pageable pageable);

    /** Quantidade em ordens de produção em rascunho destinadas à subárvore do local. */
    @Query("""
            select coalesce(sum(o.quantity), 0) from ManufacturingOrder o
            where o.product.id = :productId and o.state = com.estoque.domain.ProductionState.DRAFT
              and (o.location.fullName = :path or o.location.fullName like concat(:path, '/%'))
            """)
    BigDecimal sumDraftQuantity(@Param("productId") Long productId, @Param("path") String path);

    long countByState(ProductionState state);
}
