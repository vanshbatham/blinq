package com.urlshortener.blinq.service;

import com.urlshortener.blinq.dto.CreateLinkRequest;
import com.urlshortener.blinq.dto.GeoIpResponse;
import com.urlshortener.blinq.entity.Analytics;
import com.urlshortener.blinq.entity.Link;
import com.urlshortener.blinq.entity.User;
import com.urlshortener.blinq.repository.AnalyticsRepository;
import com.urlshortener.blinq.repository.LinkRepository;
import com.urlshortener.blinq.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import nl.basjes.parse.useragent.UserAgent;
import nl.basjes.parse.useragent.UserAgentAnalyzer;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class LinkService {

    private final LinkRepository linkRepository;
    private final UserRepository userRepository;
    private final AnalyticsRepository analyticsRepository;
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int SHORT_CODE_LENGTH = 6;
    private static final SecureRandom RANDOM = new SecureRandom();

    // --- UPDATED SERVICES for Analytics ---
    private final UserAgentAnalyzer userAgentAnalyzer;
    private final RestTemplate restTemplate;

    public LinkService(LinkRepository linkRepository, UserRepository userRepository, AnalyticsRepository analyticsRepository, RestTemplateBuilder restTemplateBuilder) {
        this.linkRepository = linkRepository;
        this.userRepository = userRepository;
        this.analyticsRepository = analyticsRepository;

        // Initialize RestTemplate for making API calls to ip-api.com
        this.restTemplate = restTemplateBuilder.build();

        // Initialize User Agent Analyzer (no change here)
        this.userAgentAnalyzer = UserAgentAnalyzer
                .newBuilder()
                .withField(UserAgent.DEVICE_CLASS)
                .build();
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
                .title(request.getTitle())
                .owner(owner)
                .expiryDate(LocalDateTime.now().plusYears(1))
                .build();

        return linkRepository.save(link);
    }

    public Optional<Link> getOriginalUrl(String shortCode) {
        return linkRepository.findByShortCode(shortCode);
    }

    // --- UPGRADED recordClick METHOD ---
    public void recordClick(Link link, HttpServletRequest request) {
        String ipAddress = request.getRemoteAddr();
        String userAgentString = request.getHeader("User-Agent");

        // Default values
        String city = "Unknown";
        String country = "Unknown";

        // GeoIP API Call
        try {
            // Use a public IP for testing if you are running locally
            if (ipAddress.equals("127.0.0.1") || ipAddress.equals("0:0:0:0:0:0:0:1")) {
                ipAddress = "8.8.8.8"; // Google's public DNS for testing
            }
            String apiUrl = "http://ip-api.com/json/" + ipAddress;
            GeoIpResponse response = restTemplate.getForObject(apiUrl, GeoIpResponse.class);
            if (response != null && "success".equals(response.status())) {
                city = response.city();
                country = response.country();
            }
        } catch (Exception e) {
            // Silently fail if the API call fails or IP is unresolvable
        }

        // User Agent Parsing
        String deviceType = "Unknown";
        if (userAgentString != null) {
            UserAgent agent = userAgentAnalyzer.parse(userAgentString);
            deviceType = agent.getValue(UserAgent.DEVICE_CLASS);
        }

        Analytics analytics = Analytics.builder()
                .link(link)
                .ipAddress(ipAddress)
                .userAgent(userAgentString)
                .referrer(request.getHeader("Referer"))
                .deviceType(deviceType)
                .city(city)
                .country(country)
                .build();
        analyticsRepository.save(analytics);
    }

    private String generateUniqueShortCode() {
        String code;
        do {
            StringBuilder sb = new StringBuilder(SHORT_CODE_LENGTH);
            for (int i = 0; i < SHORT_CODE_LENGTH; i++) {
                sb.append(CHARACTERS.charAt(RANDOM.nextInt(CHARACTERS.length())));
            }
            code = sb.toString();
        } while (linkRepository.findByShortCode(code).isPresent());
        return code;
    }

    public List<Analytics> getAnalytics(Long linkId, String requesterEmail) {
        Link link = linkRepository.findById(linkId)
                .orElseThrow(() -> new RuntimeException("Link not found"));

        if (!link.getOwner().getEmail().equals(requesterEmail)) {
            throw new SecurityException("User is not authorized to view analytics for this link");
        }

        return analyticsRepository.findByLink(link);
    }
}