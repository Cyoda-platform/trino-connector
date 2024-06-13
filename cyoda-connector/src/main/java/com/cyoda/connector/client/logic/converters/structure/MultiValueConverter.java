package com.cyoda.connector.client.logic.converters.structure;

import com.cyoda.connector.client.types.IDataType;
import io.trino.spi.type.Type;

import java.util.Iterator;

public abstract class MultiValueConverter<T,E,P extends Type> extends AbstractValueConverter<T> {

    private final String columnName; // OK since multi value converters have prototype scope
    public MultiValueConverter(IDataType<T> dataType, String columnName) {
        super(dataType);
        this.columnName = columnName;
    }

    protected abstract Iterator<E> getIterator(T value);
    protected abstract String stringifyElement(E element);

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

}
