package com.cyoda.presto.client.logic.converters.structure;

import com.facebook.presto.common.block.BlockBuilder;
import com.facebook.presto.common.type.ArrayType;
import com.facebook.presto.common.type.Type;

import java.util.Collection;
import java.util.Iterator;

public abstract class CollectionConverter<T extends Collection<E>, E> extends MultiValueConverter<T, E, ArrayType> {

    private final SingleValueConverter<E> elementConverter;

    public CollectionConverter(SingleValueConverter<E> elementConverter){
        this.elementConverter = elementConverter;
    }

    @Override
    protected void writeElement(ArrayType type, BlockBuilder elementBuilder, E value) {
        Type elementType = type.getElementType();
        elementConverter.writeValue(elementType, elementBuilder, value);
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
