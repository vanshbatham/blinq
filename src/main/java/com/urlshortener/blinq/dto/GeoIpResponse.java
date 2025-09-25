package com.urlshortener.blinq.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true) // Ignores extra fields in the JSON
public record GeoIpResponse(String status, String country, String city) {
}