package com.bookingTicket.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    // Comma-separated list, e.g. "http://localhost:5173,https://my-frontend.example"
    @Value("${app.cors.allowed-origins:http://localhost:5173}")
    private String[] allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "OPTIONS")
                // Browsers hide every response header except a short safe list.
                // The UI reads both of these, so they must be exposed.
                .exposedHeaders("X-Served-By", "Date")
                .maxAge(3600);
    }
}