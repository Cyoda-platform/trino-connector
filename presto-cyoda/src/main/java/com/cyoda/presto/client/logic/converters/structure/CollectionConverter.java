package com.cyoda.presto.client.logic.converters.structure;

import com.cyoda.presto.client.types.DataType;
import io.trino.spi.block.BlockBuilder;
import io.trino.spi.type.ArrayType;
import io.trino.spi.type.Type;

import java.util.Collection;
import java.util.Iterator;

public abstract class CollectionConverter<T extends Collection<E>, E> extends MultiValueConverter<T, E, ArrayType> {

    private final SingleValueConverter<E> elementConverter;

    public CollectionConverter(String columnName, DataType dataType, SingleValueConverter<E> elementConverter){
        super(dataType, columnName);
        this.elementConverter = elementConverter;
    }

    @Override
    protected void writeElement(ArrayType type, BlockBuilder elementBuilder, E value) {
        Type elementType = type.getElementType();
        elementConverter.writeCyodaNativeFromCollection(elementType, elementBuilder, value, getColumnName());
    }

    @Override
    protected Iterator<E> getIterator(T value) {
        return value.iterator();
    }

    @Override
    protected String stringifyElement(E element) {
        return elementConverter.stringify(element);
    }
}
