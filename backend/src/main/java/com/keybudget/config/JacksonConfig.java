package com.keybudget.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

/**
 * Registers custom Jackson serializers/deserializers for types not handled
 * by the default JavaTimeModule (e.g. {@link YearMonth} as "YYYY-MM" string).
 */
@Configuration
public class JacksonConfig {

    private static final DateTimeFormatter YEAR_MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

    @Bean
    public SimpleModule yearMonthModule() {
        SimpleModule module = new SimpleModule("YearMonthModule");

        module.addSerializer(YearMonth.class, new StdSerializer<>(YearMonth.class) {
            @Override
            public void serialize(YearMonth value, JsonGenerator gen, SerializerProvider provider)
                    throws IOException {
                gen.writeString(value.format(YEAR_MONTH_FORMAT));
            }
        });

        module.addDeserializer(YearMonth.class, new StdDeserializer<>(YearMonth.class) {
            @Override
            public YearMonth deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
                return YearMonth.parse(p.getText(), YEAR_MONTH_FORMAT);
            }
        });

        return module;
    }
}
