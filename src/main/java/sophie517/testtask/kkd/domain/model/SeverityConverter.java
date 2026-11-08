package sophie517.testtask.kkd.domain.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Locale;

@Converter(autoApply = true)
public class SeverityConverter implements AttributeConverter<Severity, String> {
    @Override
    public String convertToDatabaseColumn(Severity value) {
        return value == null ? null : value.name().toLowerCase(Locale.ROOT);
    }

    @Override
    public Severity convertToEntityAttribute(String value) {
        return value == null ? null : Severity.valueOf(value.toUpperCase(Locale.ROOT));
    }
}
