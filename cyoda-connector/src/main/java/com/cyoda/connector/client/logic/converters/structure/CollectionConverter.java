package com.cyoda.connector.client.logic.converters.structure;

import com.cyoda.connector.client.logic.converters.PrestoValueConverter;
import com.cyoda.connector.client.treenode.dto.conditions.AbstractTrinoConditionDto;
import com.cyoda.connector.client.treenode.dto.conditions.ArrayTrinoConditionDto;
import com.cyoda.connector.client.treenode.dto.conditions.Operation;
import com.cyoda.connector.client.types.DataType;
import io.trino.spi.block.ArrayBlockBuilder;
import io.trino.spi.block.BlockBuilder;
import io.trino.spi.block.ValueBlock;
import io.trino.spi.type.ArrayType;
import io.trino.spi.type.Type;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
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



    @Override
    public AbstractTrinoConditionDto toCondition(Operation operation, Type trinoType, Object nativeValue) {
        List<E> values = elementConverter.blockToNativeList(nativeValue, trinoType);
        return new ArrayTrinoConditionDto(operation, values.stream().map(elementConverter::stringify).toList());
    }
}
