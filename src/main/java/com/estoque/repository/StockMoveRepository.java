package com.estoque.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.estoque.domain.Product;
import com.estoque.domain.StockMove;

public interface StockMoveRepository extends JpaRepository<StockMove, Long> {

    /** Entradas confirmadas (ainda não concluídas) na subárvore do local. */
    @Query("""
            select coalesce(sum(m.quantity), 0) from StockMove m
            where m.product.id = :productId and m.state = com.estoque.domain.MoveState.CONFIRMED
              and (m.destLocation.fullName = :path or m.destLocation.fullName like concat(:path, '/%'))
              and not (m.sourceLocation.fullName = :path or m.sourceLocation.fullName like concat(:path, '/%'))
            """)
    BigDecimal sumIncoming(@Param("productId") Long productId, @Param("path") String path);

    /** Saídas confirmadas (ainda não concluídas) da subárvore do local. */
    @Query("""
            select coalesce(sum(m.quantity), 0) from StockMove m
            where m.product.id = :productId and m.state = com.estoque.domain.MoveState.CONFIRMED
              and (m.sourceLocation.fullName = :path or m.sourceLocation.fullName like concat(:path, '/%'))
              and not (m.destLocation.fullName = :path or m.destLocation.fullName like concat(:path, '/%'))
            """)
    BigDecimal sumOutgoing(@Param("productId") Long productId, @Param("path") String path);

    /** Movimentos confirmados de um produto que entram ou saem da subárvore do local. */
    @Query("""
            select m from StockMove m
            where m.product.id = :productId and m.state = com.estoque.domain.MoveState.CONFIRMED
              and ((m.destLocation.fullName = :path or m.destLocation.fullName like concat(:path, '/%'))
                   or (m.sourceLocation.fullName = :path or m.sourceLocation.fullName like concat(:path, '/%')))
            order by m.date
            """)
    List<StockMove> findPending(@Param("productId") Long productId, @Param("path") String path);

    /** Produtos com saídas confirmadas a partir da subárvore do local. */
    @Query("""
            select distinct m.product from StockMove m
            where m.state = com.estoque.domain.MoveState.CONFIRMED
              and (m.sourceLocation.fullName = :path or m.sourceLocation.fullName like concat(:path, '/%'))
            """)
    List<Product> findProductsWithPendingOutgoing(@Param("path") String path);

    @Query(value = """
            select m from StockMove m join fetch m.product p join fetch m.sourceLocation s join fetch m.destLocation d
            where m.state = com.estoque.domain.MoveState.DONE
              and m.date >= :from and m.date < :to
              and (:productId = 0 or p.id = :productId)
              and (:term = '' or lower(p.name) like lower(concat('%', :term, '%'))
                   or lower(coalesce(p.sku, '')) like lower(concat('%', :term, '%'))
                   or lower(coalesce(m.reference, '')) like lower(concat('%', :term, '%')))
            """,
            countQuery = """
            select count(m) from StockMove m join m.product p
            where m.state = com.estoque.domain.MoveState.DONE
              and m.date >= :from and m.date < :to
              and (:productId = 0 or p.id = :productId)
              and (:term = '' or lower(p.name) like lower(concat('%', :term, '%'))
                   or lower(coalesce(p.sku, '')) like lower(concat('%', :term, '%'))
                   or lower(coalesce(m.reference, '')) like lower(concat('%', :term, '%')))
            """)
    Page<StockMove> searchDone(@Param("term") String term, @Param("productId") long productId,
                               @Param("from") LocalDateTime from, @Param("to") LocalDateTime to, Pageable pageable);

    List<StockMove> findByManufacturingOrderId(Long manufacturingOrderId);

    List<StockMove> findTop10ByStateOrderByDateDesc(com.estoque.domain.MoveState state);

    List<StockMove> findTop20ByProductIdAndStateOrderByDateDesc(Long productId, com.estoque.domain.MoveState state);
}
