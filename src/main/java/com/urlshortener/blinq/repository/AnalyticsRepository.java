package com.urlshortener.blinq.repository;

import com.urlshortener.blinq.entity.Analytics;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnalyticsRepository extends JpaRepository<Analytics, Long> {
}