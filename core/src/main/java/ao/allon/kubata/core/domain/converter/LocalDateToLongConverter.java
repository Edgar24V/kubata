package ao.allon.kubata.core.domain.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.time.LocalDate;
import java.time.ZoneOffset;

/**
 * Converte LocalDate para Long (epoch days) para armazenamento no SQLite.
 * Resolve o erro "Error parsing time stamp" ou problemas de conversão de data no SQLite.
 */
@Converter(autoApply = true)
public class LocalDateToLongConverter implements AttributeConverter<LocalDate, Long> {

    @Override
    public Long convertToDatabaseColumn(LocalDate date) {
        return date == null ? null : date.toEpochDay();
    }

    @Override
    public LocalDate convertToEntityAttribute(Long epochDay) {
        return epochDay == null ? null : LocalDate.ofEpochDay(epochDay);
    }
}
