package com.cyoda.presto.client.types;

import com.cyoda.presto.client.logic.converters.PrestoValueConverter;

import java.util.Enumeration;

public interface IDataType<T> {
    Class<T> getJavaType();
}
