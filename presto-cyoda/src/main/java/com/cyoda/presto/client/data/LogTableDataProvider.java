package com.cyoda.presto.client.data;

import com.cyoda.presto.CyodaSplit;
import com.cyoda.presto.client.reporting.metaproviders.StaticTableMetadata;
import com.cyoda.presto.handles.CyodaColumnHandle;
import com.cyoda.presto.handles.CyodaTableHandle;
import com.cyoda.presto.logging.LogRecordHandler;
import io.trino.spi.NodeManager;
import io.trino.spi.block.Block;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.Optional;
import java.util.logging.LogRecord;

public class LogTableDataProvider extends VirtualTableDataProvider<LogRecord> {

    private LogRecordHandler logRecordHandler = LogRecordHandler.getInstance();

    public LogTableDataProvider(NodeManager nodeManager) {
        super(nodeManager);
    }

    @Override
    public Iterable<LogRecord> getIterable(CyodaTableHandle tableHandle, CyodaSplit split) {
        return logRecordHandler.getLogRecords();
    }

    @Nullable
    @Override
    protected Object getFieldValueFromEntity(@Nonnull LogRecord entity, CyodaColumnHandle columnHandle) {
        StaticTableMetadata.LogTableColumnDef columnDef = StaticTableMetadata.LogTableColumnDef.valueOf(
                columnHandle.getColumnName().toUpperCase());
        switch (columnDef) {
            case NODE_ID -> {
                return thisNode.getNodeIdentifier();
            }
            case NODE_ADDRESS -> {
                return thisNode.getHttpUri();
            }
            case DATE -> {
                return Date.from(entity.getInstant());
            }
            case LEVEL -> {
                return entity.getLevel().toString();
            }
            case CLASS -> {
                return entity.getLoggerName();
            }
            case MESSAGE -> {
                return entity.getMessage();
            }
            case STACKTRACE -> {
                return Optional.ofNullable(entity.getThrown()).map(LogTableDataProvider::exceptionStackTraceAsString).orElse(null);
            }
        }
        throw new IllegalArgumentException("Unknown column " + columnHandle.getColumnName());
    }

    public static String exceptionStackTraceAsString(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
    private static String getStackTrace(Exception e) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement stackTraceElement : e.getStackTrace()) {
            sb.append("\n");
            sb.append(stackTraceElement);
        }
        return sb.toString();
    }

    @Override
    protected void deleteByIds(Block rowIds) {
        throw new UnsupportedOperationException("DELETE for log table is not supported");
    }
}
