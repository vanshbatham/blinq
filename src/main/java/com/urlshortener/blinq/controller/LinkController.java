package com.urlshortener.blinq.controller;

import com.urlshortener.blinq.dto.CreateLinkRequest;
import com.urlshortener.blinq.entity.Link;
import com.urlshortener.blinq.service.LinkService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
public class LinkController {

    private final LinkService linkService;

    public LinkController(LinkService linkService) {
        this.linkService = linkService;
    }

    /**
     * Creates a new short link. This is a protected endpoint.
     */
    @PostMapping("/api/v1/links")
    public ResponseEntity<Link> createLink(@RequestBody CreateLinkRequest request, Authentication authentication) {
        // Get the email of the currently logged-in user
        String ownerEmail = authentication.getName();

        Link newLink = linkService.createShortLink(request.getOriginalUrl(), request.getCustomAlias(), ownerEmail);
        return ResponseEntity.status(HttpStatus.CREATED).body(newLink);
    }

    /**
     * Redirects a short code to its original URL. This is a public endpoint.
     */
    @GetMapping("/{shortCode}")
    public ResponseEntity<?> redirect(@PathVariable String shortCode) {
        return linkService.getOriginalUrl(shortCode)
                .map(link -> ResponseEntity.status(HttpStatus.FOUND)
                        .location(URI.create(link.getOriginalUrl()))
                        .build())
                .orElse(ResponseEntity.notFound().build());
    }
}