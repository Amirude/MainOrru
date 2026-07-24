package com.ooru.repository;

import com.ooru.model.BusRoute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BusRouteRepository extends JpaRepository<BusRoute, Long> {

    @Query("SELECT b FROM BusRoute b WHERE " +
           "LOWER(b.routeNumber) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(b.fromStop) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(b.toStop) LIKE LOWER(CONCAT('%', :q, '%'))")
    List<BusRoute> search(@Param("q") String query);
}
