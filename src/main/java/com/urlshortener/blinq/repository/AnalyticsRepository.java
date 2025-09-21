package com.urlshortener.blinq.repository;

import com.urlshortener.blinq.entity.Analytics;
import com.urlshortener.blinq.entity.Link;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnalyticsRepository extends JpaRepository<Analytics, Long> {
    List<Analytics> findByLink(Link link);
}