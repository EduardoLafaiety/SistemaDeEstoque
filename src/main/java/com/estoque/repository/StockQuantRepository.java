package com.estoque.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.estoque.domain.StockQuant;

import jakarta.persistence.LockModeType;

public interface StockQuantRepository extends JpaRepository<StockQuant, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select q from StockQuant q where q.product.id = :productId and q.location.id = :locationId")
    Optional<StockQuant> findForUpdate(@Param("productId") Long productId, @Param("locationId") Long locationId);

    /** Saldos positivos na subárvore do local, bloqueados para consumo (primeiro o próprio local). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select q from StockQuant q
            where q.product.id = :productId and q.quantity > 0
              and (q.location.fullName = :path or q.location.fullName like concat(:path, '/%'))
            order by q.location.fullName
            """)
    List<StockQuant> findAvailableForUpdate(@Param("productId") Long productId, @Param("path") String path);

    Optional<StockQuant> findByProductIdAndLocationId(Long productId, Long locationId);

    @Query("""
            select coalesce(sum(q.quantity), 0) from StockQuant q
            where q.product.id = :productId
              and (q.location.fullName = :path or q.location.fullName like concat(:path, '/%'))
            """)
    BigDecimal sumInSubtree(@Param("productId") Long productId, @Param("path") String path);

    @Query("""
            select coalesce(sum(q.quantity), 0) from StockQuant q
            where q.product.id = :productId and q.location.usage = com.estoque.domain.LocationUsage.INTERNAL
            """)
    BigDecimal sumInternal(@Param("productId") Long productId);

    @Query("""
            select q from StockQuant q join fetch q.location l
            where q.product.id = :productId and q.quantity <> 0
            order by l.fullName
            """)
    List<StockQuant> findByProduct(@Param("productId") Long productId);

    @Query(value = """
            select q from StockQuant q join fetch q.product p join fetch q.location l
            where l.usage = com.estoque.domain.LocationUsage.INTERNAL and q.quantity <> 0
              and (:path = '' or l.fullName = :path or l.fullName like concat(:path, '/%'))
              and (:term = '' or lower(p.name) like lower(concat('%', :term, '%'))
                   or lower(coalesce(p.sku, '')) like lower(concat('%', :term, '%')))
            """,
            countQuery = """
            select count(q) from StockQuant q join q.product p join q.location l
            where l.usage = com.estoque.domain.LocationUsage.INTERNAL and q.quantity <> 0
              and (:path = '' or l.fullName = :path or l.fullName like concat(:path, '/%'))
              and (:term = '' or lower(p.name) like lower(concat('%', :term, '%'))
                   or lower(coalesce(p.sku, '')) like lower(concat('%', :term, '%')))
            """)
    Page<StockQuant> searchInternal(@Param("path") String path, @Param("term") String term, Pageable pageable);

    @Query("""
            select coalesce(sum(q.quantity * q.product.cost), 0) from StockQuant q
            where q.location.usage = com.estoque.domain.LocationUsage.INTERNAL
            """)
    BigDecimal totalValue();

    @Query("""
            select q from StockQuant q join fetch q.product p
            where q.location.id = :locationId and q.quantity <> 0
            order by p.name
            """)
    List<StockQuant> findByLocation(@Param("locationId") Long locationId);
}
