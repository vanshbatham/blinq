package com.urlshortener.blinq.repository;

import com.urlshortener.blinq.entity.Link;
import com.urlshortener.blinq.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LinkRepository extends JpaRepository<Link, Long> {

    // To find a link for redirection using its short code
    Optional<Link> findByShortCode(String shortCode);

    // To check if a custom alias is already in use
    boolean existsByCustomAlias(String customAlias);

    // To find all links created by a specific user
    List<Link> findByOwner(User owner);
}