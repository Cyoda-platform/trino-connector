package com.cyoda.presto.client.logic.converters.structure;

import com.cyoda.presto.client.types.IDataType;
import com.facebook.presto.common.block.BlockBuilder;
import com.facebook.presto.common.type.Type;

import javax.annotation.Nonnull;
import java.util.Iterator;

public abstract class MultiValueConverter<T,E,P extends Type> extends AbstractValueConverter<T> {

    private final String columnName; // OK since multi value converters have prototype scope
    public MultiValueConverter(IDataType<T> dataType, String columnName) {
        super(dataType);
        this.columnName = columnName;
    }

    protected abstract Iterator<E> getIterator(T value);
    protected abstract String stringifyElement(E element);

    protected abstract void writeElement(P type, BlockBuilder elementBuilder, E value);

    protected String getColumnName() {
        return columnName;
    }

    @Override
    public String stringify(T value) {
        StringBuilder sb = new StringBuilder("[");
        Iterator<E> iterator = getIterator(value);
        while (iterator.hasNext()) {
            E element = iterator.next();
            sb.append(stringifyElement(element));
            if (iterator.hasNext()){
                sb.append(", ");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    public void writeValue(Type type, BlockBuilder builder, @Nonnull T value){
        BlockBuilder elementBuilder = builder.beginBlockEntry();
        Iterator<E> iterator = getIterator(value);
        while (iterator.hasNext()) {
            E element = iterator.next();
            writeElement((P) type, elementBuilder, element);
        }
        builder.closeEntry();
    }
}
