package com.urlshortener.blinq.entity;

import jakarta.persistence.*;
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
@Table(name = "links")
public class Link extends BaseEntity {

    @Column(nullable = false, length = 2048)
    private String originalUrl;

    @Column(nullable = false, unique = true)
    private String shortCode;

    private String customAlias;

    private LocalDateTime expiryDate;

    private Long clickCount = 0L;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User owner;
}
