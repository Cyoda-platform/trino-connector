package com.cyoda.connector.client.logic.converters.structure;

import com.cyoda.connector.client.types.IDataType;
import io.trino.spi.block.BlockBuilder;
import io.trino.spi.block.VariableWidthBlock;
import io.trino.spi.predicate.NullableValue;
import io.trino.spi.type.Type;
import io.airlift.slice.Slice;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public abstract class SliceComparableValueConverter<T extends Comparable<? super T>>
        extends ComparableValueConverter<T> {

    public abstract Slice toSlice(@Nonnull T value);
    public abstract @Nonnull T fromSlice(Slice value);

    public SliceComparableValueConverter(IDataType<T> dataType) {
        super(dataType);
    }

    @Override
    public void writeValue(Type type, BlockBuilder builder, @Nonnull T value) {
        type.writeSlice(builder, toSlice(value));
    }
    @Override
    public T fromPrestoNative(Object nativeValue) {
        return fromSlice((Slice) nativeValue);
    }

    @Override
    public List<T> blockToNativeList(Object nativeBlock, Type trinoType) {
        VariableWidthBlock variableWidthBlock = (VariableWidthBlock) nativeBlock;
        ArrayList<T> res = new ArrayList<T>();
        for (int i = 0; i < variableWidthBlock.getPositionCount(); i++) {
            res.add(fromSlice(variableWidthBlock.getSlice(i)));
        }
        return res;
    }

    @Override
    public NullableValue toNullableValue(Type type, Object value) {
        return new NullableValue(type, toSlice((T)value));
    }
}
