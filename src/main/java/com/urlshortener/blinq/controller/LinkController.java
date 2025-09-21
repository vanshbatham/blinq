package com.urlshortener.blinq.controller;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.urlshortener.blinq.dto.CreateLinkRequest;
import com.urlshortener.blinq.entity.Link;
import com.urlshortener.blinq.entity.User;
import com.urlshortener.blinq.repository.LinkRepository;
import com.urlshortener.blinq.repository.UserRepository;
import com.urlshortener.blinq.service.LinkService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.Hashtable;
import java.util.List;

@RestController
public class LinkController {

    // ... (fields and constructor remain the same) ...
    private final LinkService linkService;
    private final UserRepository userRepository;
    private final LinkRepository linkRepository;
    @Value("${app.base-url}")
    private String baseUrl;

    public LinkController(LinkService linkService, UserRepository userRepository, LinkRepository linkRepository) {
        this.linkService = linkService;
        this.userRepository = userRepository;
        this.linkRepository = linkRepository;
    }


    // ... (createLink, redirect, and getAllUserLinks methods are unchanged) ...
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

    // --- NEW, RELIABLE QR CODE ENDPOINT ---
    @GetMapping(value = "/api/v1/links/{shortCode}/qr", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<?> getQrCode(@PathVariable String shortCode) {
        return linkRepository.findByShortCode(shortCode)
                .map(link -> {
                    try {
                        String fullShortUrl = baseUrl + "/" + link.getShortCode();
                        int size = 250;

                        // Create QR Code
                        Hashtable<EncodeHintType, ErrorCorrectionLevel> hintMap = new Hashtable<>();
                        hintMap.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H); // High error correction for logo
                        QRCodeWriter qrCodeWriter = new QRCodeWriter();
                        BitMatrix bitMatrix = qrCodeWriter.encode(fullShortUrl, BarcodeFormat.QR_CODE, size, size, hintMap);

                        // Convert BitMatrix to a BufferedImage
                        BufferedImage qrImage = MatrixToImageWriter.toBufferedImage(bitMatrix);

                        // Load the logo
                        InputStream logoStream = new ClassPathResource("logo.png").getInputStream();
                        BufferedImage logoImage = ImageIO.read(logoStream);

                        // Calculate the logo's position (center of the QR code)
                        int logoWidth = logoImage.getWidth();
                        int logoHeight = logoImage.getHeight();
                        int positionX = (size - logoWidth) / 2;
                        int positionY = (size - logoHeight) / 2;

                        // Overlay the logo onto the QR code
                        Graphics2D graphics = qrImage.createGraphics();
                        graphics.drawImage(logoImage, positionX, positionY, logoWidth, logoHeight, null);
                        graphics.dispose();

                        // Write the final image to a byte array
                        ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
                        ImageIO.write(qrImage, "PNG", pngOutputStream);

                        return ResponseEntity.ok(pngOutputStream.toByteArray());
                    } catch (Exception e) {
                        return ResponseEntity.<byte[]>status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                    }
                })
                .orElse(ResponseEntity.<byte[]>notFound().build());
    }
}