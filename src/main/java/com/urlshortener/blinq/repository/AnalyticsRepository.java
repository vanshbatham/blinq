package com.urlshortener.blinq.repository;

import com.urlshortener.blinq.dto.AnalyticsGroupCount;
import com.urlshortener.blinq.entity.Analytics;
import com.urlshortener.blinq.entity.Link;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AnalyticsRepository extends JpaRepository<Analytics, Long> {

    // Finds all individual click records for a link
    List<Analytics> findByLink(Link link);

    // Counts the total number of clicks for a given link
    long countByLink(Link link);

    // This query groups clicks by country and counts them
    @Query("SELECT new com.urlshortener.blinq.dto.AnalyticsGroupCount(a.country, COUNT(a)) FROM Analytics a WHERE a.link = :link GROUP BY a.country")
    List<AnalyticsGroupCount> countByCountry(@Param("link") Link link);

    // This query groups clicks by device type and counts them
    @Query("SELECT new com.urlshortener.blinq.dto.AnalyticsGroupCount(a.deviceType, COUNT(a)) FROM Analytics a WHERE a.link = :link GROUP BY a.deviceType")
    List<AnalyticsGroupCount> countByDeviceType(@Param("link") Link link);
}