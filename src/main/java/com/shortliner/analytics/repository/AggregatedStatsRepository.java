package com.shortliner.analytics.repository;

import com.shortliner.analytics.entity.AggregatedStats;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface AggregatedStatsRepository extends JpaRepository<AggregatedStats, Long> {

    Page<AggregatedStats> findByShortCodeOrderByDateDesc(String shortCode, Pageable pageable);

    Optional<AggregatedStats> findByShortCodeAndDate(String shortCode, LocalDate date);
}
