package com.example.rental_management.common;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Jackson 3 configuration (Spring Boot 4 / tools.jackson.*).
 *
 * <p>Jackson 3 bundles java.time support directly in jackson-databind — there
 * is no separate jsr310 module. DateTimeFeature replaces the Jackson 2
 * SerializationFeature.WRITE_DATES_AS_TIMESTAMPS flag.
 *
 * <p>Disabling WRITE_DATES_AS_TIMESTAMPS serializes java.time types
 * (LocalDate, LocalDateTime, YearMonth) as ISO-8601 strings so the
 * frontend can parse them correctly.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public JsonMapper objectMapper() {
        return JsonMapper.builder()
                .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();
    }
}
