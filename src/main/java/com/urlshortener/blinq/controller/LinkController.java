package com.urlshortener.blinq.controller;

import com.urlshortener.blinq.dto.CreateLinkRequest;
import com.urlshortener.blinq.entity.Link;
import com.urlshortener.blinq.entity.User;
import com.urlshortener.blinq.repository.LinkRepository;
import com.urlshortener.blinq.repository.UserRepository;
import com.urlshortener.blinq.service.LinkService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
public class LinkController {

    private final LinkService linkService;
    private final UserRepository userRepository;
    private final LinkRepository linkRepository;

    public LinkController(LinkService linkService, UserRepository userRepository, LinkRepository linkRepository) {
        this.linkService = linkService;
        this.userRepository = userRepository;
        this.linkRepository = linkRepository;
    }

    @PostMapping("/api/v1/links")
    public ResponseEntity<Link> createLink(@RequestBody CreateLinkRequest request, Authentication authentication) {
        String ownerEmail = authentication.getName();

        Link newLink = linkService.createShortLink(request, ownerEmail);
        return ResponseEntity.status(HttpStatus.CREATED).body(newLink);
    }

    @GetMapping("/{shortCode}")
    public ResponseEntity<?> redirect(@PathVariable String shortCode, HttpServletRequest request) {
        return linkService.getOriginalUrl(shortCode)
                .map(link -> {
                    // This is the new line that records the click
                    linkService.recordClick(link, request);

                    return ResponseEntity.status(HttpStatus.FOUND)
                            .location(URI.create(link.getOriginalUrl()))
                            .build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/api/v1/links")
    public ResponseEntity<List<Link>> getAllUserLinks(Authentication authentication) {
        // 1. Get the currently logged-in user
        User owner = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found during link fetch"));

        // 2. Fetch all links for that user from the database
        List<Link> links = linkRepository.findByOwner(owner);

        // 3. Return the list of links as JSON
        return ResponseEntity.ok(links);
    }
}