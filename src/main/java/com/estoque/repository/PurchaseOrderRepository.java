package com.estoque.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.estoque.domain.Location;
import com.estoque.domain.Partner;
import com.estoque.domain.PurchaseOrder;
import com.estoque.domain.PurchaseState;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

    Optional<PurchaseOrder> findFirstByPartnerAndDestLocationAndStateOrderByIdDesc(Partner partner, Location destLocation,
                                                                                    PurchaseState state);

    @Query("""
            select o from PurchaseOrder o join o.partner pa
            where o.state in :states
              and (:term = '' or lower(o.reference) like lower(concat('%', :term, '%'))
                   or lower(pa.name) like lower(concat('%', :term, '%'))
                   or lower(coalesce(o.origin, '')) like lower(concat('%', :term, '%')))
            """)
    Page<PurchaseOrder> search(@Param("states") List<PurchaseState> states, @Param("term") String term, Pageable pageable);

    /** Quantidade em solicitações de cotação (rascunho) destinadas à subárvore do local. */
    @Query("""
            select coalesce(sum(l.quantity), 0) from PurchaseOrderLine l
            where l.product.id = :productId and l.order.state = com.estoque.domain.PurchaseState.DRAFT
              and (l.order.destLocation.fullName = :path or l.order.destLocation.fullName like concat(:path, '/%'))
            """)
    BigDecimal sumDraftQuantity(@Param("productId") Long productId, @Param("path") String path);

    long countByState(PurchaseState state);
}
