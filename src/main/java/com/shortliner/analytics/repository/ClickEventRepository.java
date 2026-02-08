package com.shortliner.analytics.repository;

import com.shortliner.analytics.dto.UserAnalyticsResponse;
import com.shortliner.analytics.entity.ClickEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface ClickEventRepository extends JpaRepository<ClickEvent, UUID> {

    boolean existsByEventHash(String eventHash);

    boolean existsByShortCode(String shortCode);

    long countByShortCode(String shortCode);

    @Query("SELECT COUNT(DISTINCT e.ip) FROM ClickEvent e WHERE e.shortCode = :shortCode")
    long countDistinctIpByShortCode(@Param("shortCode") String shortCode);

    @Query("SELECT MIN(e.timestamp) FROM ClickEvent e WHERE e.shortCode = :shortCode")
    Instant findFirstClickByShortCode(@Param("shortCode") String shortCode);

    @Query("SELECT MAX(e.timestamp) FROM ClickEvent e WHERE e.shortCode = :shortCode")
    Instant findLastClickByShortCode(@Param("shortCode") String shortCode);

    @Query(value = """
            SELECT ce.short_code, CAST(ce.clicked_at AS DATE) AS click_date,
                   COUNT(*) AS click_count, COUNT(DISTINCT ce.ip) AS unique_ips
            FROM click_events ce
            GROUP BY ce.short_code, CAST(ce.clicked_at AS DATE)
            """, nativeQuery = true)
    List<Object[]> findDailyAggregates();

    @Query("""
            SELECT new com.shortliner.analytics.dto.UserAnalyticsResponse(
                e.shortCode, COUNT(e), MAX(e.timestamp))
            FROM ClickEvent e
            WHERE e.userId = :userId
            GROUP BY e.shortCode
            """)
    Page<UserAnalyticsResponse> findUserAnalytics(@Param("userId") String userId, Pageable pageable);
}
