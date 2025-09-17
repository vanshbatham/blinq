package com.urlshortener.blinq.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "analytics")
public class Analytics extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "link_id", nullable = false)
    private Link link;
    private String ipAddress;
    private String userAgent;
    private String referrer;
    private String deviceType; // e.g., "Desktop", "Mobile", "Tablet"
    private String country;
    private String city;
}