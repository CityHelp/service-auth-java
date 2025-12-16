package com.crudzaso.cityhelp.auth.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web configuration for CityHelp Auth Service.
 * Configures JSON serialization, CORS, and other web-related settings.
 *
 * CORS Configuration:
 * - Development: http://localhost:* and http://127.0.0.1:*
 * - Production: https://* (all HTTPS origins)
 * - Allows Swagger UI to work in both local and production environments
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Bean
    public MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        return new MappingJackson2HttpMessageConverter(objectMapper);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // CORS TEMPORARILY DISABLED FOR TESTING
        // To re-enable: uncomment the code below
        /*
        registry.addMapping("/**")
                .allowedOriginPatterns("http://localhost:*", "http://127.0.0.1:*", "https://*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
        */
    }
}