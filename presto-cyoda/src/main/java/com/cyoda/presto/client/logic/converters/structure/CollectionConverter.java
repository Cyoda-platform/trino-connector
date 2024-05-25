package com.cyoda.presto.client.logic.converters.structure;

import com.cyoda.presto.client.types.DataType;
import io.trino.spi.block.ArrayBlockBuilder;
import io.trino.spi.block.Block;
import io.trino.spi.block.BlockBuilder;
import io.trino.spi.type.ArrayType;
import io.trino.spi.type.Type;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public abstract class CollectionConverter<T extends Collection<E>, E> extends MultiValueConverter<T, E, ArrayType> {

    private final SingleValueConverter<E> elementConverter;

    public CollectionConverter(String columnName, DataType dataType, SingleValueConverter<E> elementConverter){
        super(dataType, columnName);
        this.elementConverter = elementConverter;
    }

    @Override
    protected Iterator<E> getIterator(T value) {
        return value.iterator();
    }

    @Override
    protected String stringifyElement(E element) {
        return elementConverter.stringify(element);
    }

    @Override
    public void writeValue(Type type, BlockBuilder builder, @NotNull T value) {
        Type elType = ((ArrayType) type).getElementType();
        ((ArrayBlockBuilder) builder).buildEntry(elementBuilder -> value.forEach(element -> {
            elementConverter.writeCyodaNativeFromCollection(elType, elementBuilder, element, getColumnName());
    }));
    }
}
