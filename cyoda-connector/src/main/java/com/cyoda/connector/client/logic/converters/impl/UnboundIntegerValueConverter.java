package com.cyoda.connector.client.logic.converters.impl;

import com.cyoda.connector.client.logic.converters.structure.StringTypeValueConverter;
import com.cyoda.connector.client.types.DataType;

import java.math.BigInteger;

public class UnboundIntegerValueConverter extends StringTypeValueConverter<BigInteger> {

    public UnboundIntegerValueConverter() {
        super(DataType.UNBOUND_INTEGER);
    }

    @Override
    protected BigInteger fromStr(String value) {
        return new BigInteger(value);
    }

    @Override
    protected String toStr(BigInteger value) {
        return value.toString();
    }
}
