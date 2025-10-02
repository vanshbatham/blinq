package com.urlshortener.blinq.controller;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.urlshortener.blinq.dto.AnalyticsGroupCount;
import com.urlshortener.blinq.dto.CreateLinkRequest;
import com.urlshortener.blinq.entity.Analytics;
import com.urlshortener.blinq.entity.Link;
import com.urlshortener.blinq.entity.User;
import com.urlshortener.blinq.repository.AnalyticsRepository;
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
import java.util.Map;

@RestController
public class LinkController {

    private final LinkService linkService;
    private final UserRepository userRepository;
    private final LinkRepository linkRepository;
    private final AnalyticsRepository analyticsRepository; // Added for summary endpoint

    @Value("${app.base-url}")
    private String baseUrl;

    public LinkController(LinkService linkService, UserRepository userRepository, LinkRepository linkRepository, AnalyticsRepository analyticsRepository) {
        this.linkService = linkService;
        this.userRepository = userRepository;
        this.linkRepository = linkRepository;
        this.analyticsRepository = analyticsRepository; // Added for summary endpoint
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
                    // CORRECT: Click is recorded here when the link is used.
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

                        // This correctly returns a ResponseEntity<byte[]>
                        return ResponseEntity.ok(pngOutputStream.toByteArray());
                    } catch (Exception e) {
                        // This returns a generic ResponseEntity, which is fine now
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                    }
                })
                // This also returns a generic ResponseEntity
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/api/v1/links/{linkId}/analytics")
    public ResponseEntity<List<Analytics>> getLinkAnalytics(@PathVariable Long linkId, Authentication authentication) {
        String requesterEmail = authentication.getName();
        try {
            List<Analytics> analytics = linkService.getAnalytics(linkId, requesterEmail);
            return ResponseEntity.ok(analytics);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/api/v1/links/{linkId}/analytics/summary")
    public ResponseEntity<?> getLinkAnalyticsSummary(@PathVariable Long linkId, Authentication authentication) {
        String requesterEmail = authentication.getName();

        Link link = linkRepository.findById(linkId)
                .orElse(null);

        if (link == null) {
            return ResponseEntity.notFound().build();
        }

        if (!link.getOwner().getEmail().equals(requesterEmail)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "User is not authorized"));
        }

        List<AnalyticsGroupCount> byCountry = analyticsRepository.countByCountry(link);
        List<AnalyticsGroupCount> byDevice = analyticsRepository.countByDeviceType(link);
        long totalClicks = analyticsRepository.countByLink(link);

        Map<String, Object> summary = Map.of(
                "totalClicks", totalClicks,
                "byCountry", byCountry,
                "byDevice", byDevice
        );

        return ResponseEntity.ok(summary);
    }
}