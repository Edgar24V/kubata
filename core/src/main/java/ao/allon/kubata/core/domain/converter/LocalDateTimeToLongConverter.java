package ao.allon.kubata.core.domain.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Converte LocalDateTime para Long (epoch milliseconds) para armazenamento no SQLite.
 * Resolve o erro "Error parsing time stamp" causado por dados numéricos onde o driver espera strings.
 */
@Converter(autoApply = true)
public class LocalDateTimeToLongConverter implements AttributeConverter<LocalDateTime, Long> {

    @Override
    public Long convertToDatabaseColumn(LocalDateTime ldt) {
        return ldt == null ? null : ldt.toInstant(ZoneOffset.UTC).toEpochMilli();
    }

    @Override
    public LocalDateTime convertToEntityAttribute(Long epochMilli) {
        return epochMilli == null ? null : 
               LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMilli), ZoneOffset.UTC);
    }
}
