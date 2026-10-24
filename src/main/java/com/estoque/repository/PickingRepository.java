package com.estoque.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.estoque.domain.Picking;
import com.estoque.domain.PickingState;
import com.estoque.domain.PickingType;

public interface PickingRepository extends JpaRepository<Picking, Long> {

    /** Lista filtrada; {@code states} vazio não é permitido, passe todos os estados para "todos". */
    @Query("""
            select p from Picking p left join p.partner pa
            where p.type = :type and p.state in :states
              and (:term = '' or lower(p.reference) like lower(concat('%', :term, '%'))
                   or lower(coalesce(pa.name, '')) like lower(concat('%', :term, '%'))
                   or lower(coalesce(p.origin, '')) like lower(concat('%', :term, '%')))
            """)
    Page<Picking> search(@Param("type") PickingType type, @Param("states") List<PickingState> states,
                         @Param("term") String term, Pageable pageable);

    long countByTypeAndState(PickingType type, PickingState state);

    long countByTypeAndStateAndScheduledDateBefore(PickingType type, PickingState state, LocalDateTime date);

    List<Picking> findByPurchaseOrderId(Long purchaseOrderId);
}
