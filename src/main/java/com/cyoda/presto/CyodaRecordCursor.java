package com.cyoda.presto;

import com.cyoda.presto.handles.CyodaColumnHandle;
import com.facebook.presto.common.type.Type;
import com.facebook.presto.spi.RecordCursor;
import com.google.common.io.ByteSource;
import io.airlift.slice.Slice;

import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;

public class CyodaRecordCursor implements RecordCursor {

    // TODO: AccumuloRecordCursor might be a good place to look
    private long bytesRead;
    private long nanoStart;
    private long nanoEnd;

    private final List<CyodaColumnHandle> columnHandles;


    public CyodaRecordCursor(List<CyodaColumnHandle> columnHandles, ByteSource byteSource) {
        this.columnHandles = columnHandles;
    }

    @Override
    public long getCompletedBytes() {
        return bytesRead;
    }

    @Override
    public long getReadTimeNanos() {
        long thisNanoEnd = nanoEnd == 0 ? System.nanoTime() : nanoEnd;
        return nanoStart > 0L ? thisNanoEnd - nanoStart : 0L;
    }

    @Override
    public Type getType(int field) {
        checkArgument(field >= 0 && field < columnHandles.size(), "Invalid field index");
        return columnHandles.get(field).getColumnType();
    }

    @Override
    public boolean advanceNextPosition() {
        if (nanoStart == 0) {
            nanoStart = System.nanoTime();
        }

        // TODO: Calculate bytesRead in this method

        throw new UnsupportedOperationException("not yet implemented");
    }

    @Override
    public boolean getBoolean(int field) {
        throw new UnsupportedOperationException("not yet implemented");
    }

    @Override
    public long getLong(int field) {
        throw new UnsupportedOperationException("not yet implemented");
    }

    @Override
    public double getDouble(int field) {
        throw new UnsupportedOperationException("not yet implemented");
    }

    @Override
    public Slice getSlice(int field) {
        throw new UnsupportedOperationException("not yet implemented");
    }

    @Override
    public Object getObject(int field) {
        throw new UnsupportedOperationException("not yet implemented");
    }

    @Override
    public boolean isNull(int field) {
        throw new UnsupportedOperationException("not yet implemented");
    }

    @Override
    public void close() {
        nanoEnd = System.nanoTime();

        throw new UnsupportedOperationException("not yet implemented");
    }
}
