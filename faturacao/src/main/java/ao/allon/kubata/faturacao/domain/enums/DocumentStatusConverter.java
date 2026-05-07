package ao.allon.kubata.faturacao.domain.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class DocumentStatusConverter implements AttributeConverter<DocumentStatus, String> {

    @Override
    public String convertToDatabaseColumn(DocumentStatus attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getCodigo();
    }

    @Override
    public DocumentStatus convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return DocumentStatus.fromCodigo(dbData);
    }
}
