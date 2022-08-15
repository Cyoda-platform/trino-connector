package com.cyoda.presto.client.logic.converters.structure;

import com.cyoda.presto.client.logic.converters.PrestoValueConverter;
import com.facebook.presto.common.block.BlockBuilder;
import com.facebook.presto.common.type.Type;

import java.util.Iterator;

public abstract class MultiValueConverter<T,E,P extends Type> implements PrestoValueConverter<T> {

    protected abstract Iterator<E> getIterator(T value);
    protected abstract String stringifyElement(E element);

    protected abstract void writeElement(P type, BlockBuilder elementBuilder, E value);

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

    public void writeValue(P type, BlockBuilder builder, T value){
        BlockBuilder elementBuilder = builder.beginBlockEntry();
        if (value != null) {
            Iterator<E> iterator = getIterator(value);
            while (iterator.hasNext()) {
                E element = iterator.next();
                writeElement(type, elementBuilder, element);
            }
        }
        builder.closeEntry();
    }
}
