package com.urlshortener.blinq.controller;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.urlshortener.blinq.dto.CreateLinkRequest;
import com.urlshortener.blinq.entity.Analytics;
import com.urlshortener.blinq.entity.Link;
import com.urlshortener.blinq.entity.User;
import com.urlshortener.blinq.repository.LinkRepository;
import com.urlshortener.blinq.repository.UserRepository;
import com.urlshortener.blinq.service.LinkService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.util.List;

@RestController
public class LinkController {

    private final LinkService linkService;
    private final UserRepository userRepository;
    private final LinkRepository linkRepository;

    @Value("${app.base-url}") // <-- Injects the base URL from application.properties
    private String baseUrl;

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
                    linkService.recordClick(link, request);
                    return ResponseEntity.status(HttpStatus.FOUND)
                            .location(URI.create(link.getOriginalUrl()))
                            .build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/api/v1/links")
    public ResponseEntity<List<Link>> getAllUserLinks(Authentication authentication) {
        User owner = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found during link fetch"));
        List<Link> links = linkRepository.findByOwner(owner);
        return ResponseEntity.ok(links);
    }

    // --- ADD THIS NEW ENDPOINT ---
    @GetMapping(value = "/api/v1/links/{shortCode}/qr", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<?> getQrCode(@PathVariable String shortCode) {
        return linkRepository.findByShortCode(shortCode)
                .map(link -> {
                    try {
                        String fullShortUrl = baseUrl + "/" + link.getShortCode();
                        QRCodeWriter qrCodeWriter = new QRCodeWriter();
                        BitMatrix bitMatrix = qrCodeWriter.encode(fullShortUrl, BarcodeFormat.QR_CODE, 250, 250);

                        ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
                        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
                        byte[] pngData = pngOutputStream.toByteArray();

                        return ResponseEntity.ok(pngData);
                    } catch (Exception e) {
                        // This is the newly corrected line
                        return ResponseEntity.<byte[]>status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                    }
                })
                .orElse(ResponseEntity.<byte[]>notFound().build());
    }
    
    @GetMapping("/api/v1/links/{linkId}/analytics")
    public ResponseEntity<List<Analytics>> getLinkAnalytics(@PathVariable Long linkId, Authentication authentication) {
        String requesterEmail = authentication.getName();
        try {
            List<Analytics> analytics = linkService.getAnalytics(linkId, requesterEmail);
            return ResponseEntity.ok(analytics);
        } catch (SecurityException e) {
            // Return a 403 Forbidden status if the user is not the owner
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (RuntimeException e) {
            // Return a 404 Not Found status if the link doesn't exist
            return ResponseEntity.notFound().build();
        }
    }
}