package com.urlshortener.blinq.service;

import com.urlshortener.blinq.dto.CreateLinkRequest;
import com.urlshortener.blinq.entity.Analytics;
import com.urlshortener.blinq.entity.Link;
import com.urlshortener.blinq.entity.User;
import com.urlshortener.blinq.repository.AnalyticsRepository;
import com.urlshortener.blinq.repository.LinkRepository;
import com.urlshortener.blinq.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class LinkService {

    private final LinkRepository linkRepository;
    private final UserRepository userRepository;
    private final AnalyticsRepository analyticsRepository;

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int SHORT_CODE_LENGTH = 6;
    private static final SecureRandom RANDOM = new SecureRandom();

    public LinkService(LinkRepository linkRepository, UserRepository userRepository, AnalyticsRepository analyticsRepository) {
        this.linkRepository = linkRepository;
        this.userRepository = userRepository;
        this.analyticsRepository = analyticsRepository;
    }

    @Transactional
    public Link createShortLink(CreateLinkRequest request, String ownerEmail) {
        User owner = userRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String shortCode;
        String customAlias = request.getCustomAlias();

        if (customAlias != null && !customAlias.isEmpty()) {
            if (linkRepository.findByShortCode(customAlias).isPresent() || linkRepository.existsByCustomAlias(customAlias)) {
                throw new IllegalArgumentException("Custom alias is already in use.");
            }
            shortCode = customAlias;
        } else {
            shortCode = generateUniqueShortCode();
        }

        Link link = Link.builder()
                .originalUrl(request.getOriginalUrl())
                .shortCode(shortCode)
                .customAlias(customAlias)
                .title(request.getTitle()) // <-- ADD THIS LINE
                .owner(owner)
                .expiryDate(LocalDateTime.now().plusYears(1))
                .build();

        return linkRepository.save(link);
    }

    public Optional<Link> getOriginalUrl(String shortCode) {
        return linkRepository.findByShortCode(shortCode);
    }

    public void recordClick(Link link, HttpServletRequest request) {
        Analytics analytics = Analytics.builder()
                .link(link)
                .ipAddress(request.getRemoteAddr())
                .userAgent(request.getHeader("User-Agent"))
                .referrer(request.getHeader("Referer"))
                .build();
        analyticsRepository.save(analytics);
    }

    /**
     * Generates a random short code and ensures it is unique in the database.
     *
     * @return A unique 6-character short code.
     */
    private String generateUniqueShortCode() {
        String code;
        do {
            StringBuilder sb = new StringBuilder(SHORT_CODE_LENGTH);
            for (int i = 0; i < SHORT_CODE_LENGTH; i++) {
                sb.append(CHARACTERS.charAt(RANDOM.nextInt(CHARACTERS.length())));
            }
            code = sb.toString();
        } while (linkRepository.findByShortCode(code).isPresent()); // Loop until a unique code is found
        return code;
    }
}