package com.worldcup.entity;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Converter
@Slf4j
public class RankedPercentagesConverter implements AttributeConverter<Map<Integer, BigDecimal>, String> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(Map<Integer, BigDecimal> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return null;
        }
        try {
            // Convert Map<Integer, BigDecimal> to JSON string
            // We need to convert keys to strings for JSON
            Map<String, String> stringMap = new HashMap<>();
            for (Map.Entry<Integer, BigDecimal> entry : attribute.entrySet()) {
                stringMap.put(entry.getKey().toString(), entry.getValue().toString());
            }
            return objectMapper.writeValueAsString(stringMap);
        } catch (Exception e) {
            log.error("Error converting ranked percentages to database column: {}", e.getMessage(), e);
            return null;
        }
    }

    @Override
    public Map<Integer, BigDecimal> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return new HashMap<>();
        }
        try {
            // Parse JSON string back to Map<Integer, BigDecimal>
            Map<String, String> stringMap = objectMapper.readValue(dbData, new TypeReference<Map<String, String>>() {});
            Map<Integer, BigDecimal> result = new HashMap<>();
            for (Map.Entry<String, String> entry : stringMap.entrySet()) {
                result.put(Integer.parseInt(entry.getKey()), new BigDecimal(entry.getValue()));
            }
            return result;
        } catch (Exception e) {
            log.error("Error converting ranked percentages from database column: {}", e.getMessage(), e);
            return new HashMap<>();
        }
    }
}

