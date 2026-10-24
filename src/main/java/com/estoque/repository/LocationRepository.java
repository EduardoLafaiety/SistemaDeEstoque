package com.estoque.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.estoque.domain.Location;
import com.estoque.domain.LocationUsage;

public interface LocationRepository extends JpaRepository<Location, Long> {

    List<Location> findAllByOrderByFullName();

    List<Location> findByActiveTrueOrderByFullName();

    List<Location> findByUsageAndActiveTrueOrderByFullName(LocationUsage usage);

    Optional<Location> findFirstByUsageOrderById(LocationUsage usage);

    boolean existsByFullName(String fullName);

    /** O próprio local e todos os seus descendentes. */
    @Query("select l from Location l where l.fullName = :path or l.fullName like concat(:path, '/%') order by l.fullName")
    List<Location> findSubtree(@Param("path") String path);
}
