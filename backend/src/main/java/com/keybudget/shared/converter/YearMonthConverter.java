package com.keybudget.shared.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.YearMonth;

/**
 * JPA AttributeConverter that persists {@link YearMonth} as a "YYYY-MM" string column.
 * Auto-applied to all entity attributes of type {@code YearMonth}.
 */
@Converter(autoApply = true)
public class YearMonthConverter implements AttributeConverter<YearMonth, String> {

    @Override
    public String convertToDatabaseColumn(YearMonth attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.toString(); // ISO-8601: "2026-03"
    }

    @Override
    public YearMonth convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        return YearMonth.parse(dbData);
    }
}
