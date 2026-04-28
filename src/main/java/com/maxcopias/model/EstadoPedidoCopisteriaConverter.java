package com.maxcopias.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class EstadoPedidoCopisteriaConverter implements AttributeConverter<EstadoPedidoCopisteria, String> {

    @Override
    public String convertToDatabaseColumn(EstadoPedidoCopisteria attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public EstadoPedidoCopisteria convertToEntityAttribute(String dbData) {
        return EstadoPedidoCopisteria.fromDatabaseValue(dbData);
    }
}
