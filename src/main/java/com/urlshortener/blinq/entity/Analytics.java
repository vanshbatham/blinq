package com.urlshortener.blinq.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;


import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "analytics")
public class Analytics extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "link_id")
    private Link link;

    private LocalDateTime accessedAt;

    private String ipAddress;

    private String userAgent;

    private String referrer;
}
