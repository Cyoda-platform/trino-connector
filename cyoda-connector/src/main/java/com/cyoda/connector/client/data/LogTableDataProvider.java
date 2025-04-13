package com.cyoda.connector.client.data;

import com.cyoda.connector.client.reporting.metaproviders.StaticTableMetadata;
import com.cyoda.connector.handles.CyodaColumnHandle;
import com.cyoda.connector.logging.LogRecordHandler;
import io.trino.spi.NodeManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.Optional;
import java.util.logging.LogRecord;

public class LogTableDataProvider extends VirtualTableDataProvider<LogRecord> {

    public LogTableDataProvider(NodeManager nodeManager) {
        super(nodeManager, () -> LogRecordHandler.getInstance().getLogRecords());
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
                return thisNode.getHostAndPort().toString();
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

}
