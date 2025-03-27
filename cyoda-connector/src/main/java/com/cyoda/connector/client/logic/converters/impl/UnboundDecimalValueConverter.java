package com.cyoda.connector.client.logic.converters.impl;

import com.cyoda.connector.client.logic.converters.structure.StringTypeValueConverter;
import com.cyoda.connector.client.types.DataType;

import java.math.BigDecimal;

public class UnboundDecimalValueConverter extends StringTypeValueConverter<BigDecimal> {

    public UnboundDecimalValueConverter() {
        super(DataType.UNBOUND_DECIMAL);
    }

    @Override
    protected BigDecimal fromStr(String value) {
        return new BigDecimal(value);
    }

    @Override
    protected String toStr(BigDecimal value) {
        return value.stripTrailingZeros().toString();
    }
}
