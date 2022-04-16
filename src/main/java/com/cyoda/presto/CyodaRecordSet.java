package com.cyoda.presto;

import com.cyoda.presto.handles.CyodaColumnHandle;
import com.cyoda.presto.http.QueryRunner;
import com.facebook.airlift.http.client.HttpUriBuilder;
import com.facebook.airlift.http.client.Request;
import com.facebook.presto.common.type.Type;
import com.facebook.presto.spi.RecordCursor;
import com.facebook.presto.spi.RecordSet;
import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteSource;

import java.util.List;

import static com.facebook.airlift.http.client.HttpUriBuilder.uriBuilderFrom;
import static com.facebook.airlift.http.client.Request.Builder.preparePost;
import static com.facebook.airlift.http.client.StaticBodyGenerator.createStaticBodyGenerator;
import static java.util.Objects.requireNonNull;

public class CyodaRecordSet implements RecordSet {
    private final List<CyodaColumnHandle> columnHandles;
    private final List<Type> columnTypes;

    // TODO: This should be something streamed from Reporting API
    private final ByteSource byteSource;
    private final CyodaClient client;

    public CyodaRecordSet(CyodaClient client, CyodaSplit split, ImmutableList<CyodaColumnHandle> columnHandles) {

        this.client = requireNonNull(client, "client is null");
        requireNonNull(split, "split is null");

        this.columnHandles = requireNonNull(columnHandles, "column handles is null");
        ImmutableList.Builder<Type> types = ImmutableList.builder();
        for (CyodaColumnHandle column : columnHandles) {
            types.add(column.getColumnType());
        }
        this.columnTypes = types.build();
        byteSource = createByteSource();
    }

    private ByteSource createByteSource() {
        QueryRunner queryRunner = client.getQueryRunner();
        throw new UnsupportedOperationException("not yet implemented");
    }

    @Override
    public List<Type> getColumnTypes()
    {
        return columnTypes;
    }

    @Override
    public RecordCursor cursor()
    {
        return new CyodaRecordCursor(columnHandles, byteSource);
    }
}
