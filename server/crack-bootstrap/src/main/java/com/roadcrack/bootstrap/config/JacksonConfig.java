package com.roadcrack.bootstrap.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Jackson configuration for LocalDateTime serialization/deserialization.
 * Supports multiple datetime formats including ISO-8601 without milliseconds.
 */
@Configuration
public class JacksonConfig {

    private static final DateTimeFormatter[] DATE_FORMATS = {
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,           // 2026-07-13T11:40:00
            DateTimeFormatter.ISO_OFFSET_DATE_TIME,          // 2026-07-13T11:40:00+08:00
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),  // 2026-07-13 11:40:00
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"),  // 2026-07-13T11:40:00.000
    };

    private static final DateTimeFormatter OUTPUT_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        return builder -> {
            JavaTimeModule javaTimeModule = new JavaTimeModule();
            // Serializer - output format
            javaTimeModule.addSerializer(LocalDateTime.class,
                    new LocalDateTimeSerializer(OUTPUT_FORMAT));
            // Deserializer - support multiple input formats
            javaTimeModule.addDeserializer(LocalDateTime.class,
                    new MultiFormatLocalDateTimeDeserializer());
            builder.modules(javaTimeModule);
            builder.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            builder.timeZone("Asia/Shanghai");
        };
    }

    /**
     * Custom deserializer that tries multiple datetime formats.
     */
    private static class MultiFormatLocalDateTimeDeserializer extends LocalDateTimeDeserializer {
        public MultiFormatLocalDateTimeDeserializer() {
            super(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }

        @Override
        public LocalDateTime deserialize(com.fasterxml.jackson.core.JsonParser parser,
                                          com.fasterxml.jackson.databind.DeserializationContext context)
                throws java.io.IOException {
            String value = parser.getValueAsString();
            if (value == null || value.trim().isEmpty()) {
                return null;
            }
            // Try each format until one works
            for (DateTimeFormatter fmt : DATE_FORMATS) {
                try {
                    return LocalDateTime.parse(value, fmt);
                } catch (Exception ignored) {
                    // try next format
                }
            }
            throw context.wrongTokenException(parser, LocalDateTime.class,
                    com.fasterxml.jackson.core.JsonToken.VALUE_STRING,
                    "Cannot parse datetime: " + value);
        }
    }
}
