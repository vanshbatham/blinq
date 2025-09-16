package com.urlshortener.blinq.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateLinkRequest {
    private String originalUrl;
    private String customAlias;
}