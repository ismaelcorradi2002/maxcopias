package com.maxcopias.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class EstadoPedidoTiendaConverter implements AttributeConverter<EstadoPedidoTienda, String> {

    @Override
    public String convertToDatabaseColumn(EstadoPedidoTienda attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public EstadoPedidoTienda convertToEntityAttribute(String dbData) {
        return EstadoPedidoTienda.fromDatabaseValue(dbData);
    }
}
