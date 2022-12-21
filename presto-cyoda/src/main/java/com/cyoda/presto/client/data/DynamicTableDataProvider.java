package com.cyoda.presto.client.data;

import com.cyoda.presto.SizeListener;
import com.cyoda.presto.auth.AuthContext;
import com.cyoda.presto.client.logic.CompoundPredicateNode;
import com.cyoda.presto.client.reporting.data.ReportRowsApiHandler;
import com.cyoda.presto.client.reporting.data.RowHandle;
import com.cyoda.presto.client.reporting.data.RowsRequestKey;
import com.cyoda.presto.client.reporting.groups.GroupsRequestKey;
import com.cyoda.presto.client.reporting.groups.ReportGroupsApiHandler;
import com.cyoda.presto.client.reporting.meta.ReportHistoryApiHandler;
import com.cyoda.presto.handles.CyodaColumnHandle;
import com.cyoda.presto.handles.CyodaTableHandle;
import reactor.core.scheduler.Schedulers;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.stream.Collectors;

import static com.cyoda.presto.client.reporting.metaproviders.StaticReportFields.ROW_GROUPING_VERSION_COLUMN;
import static com.cyoda.presto.client.reporting.metaproviders.StaticReportFields.ROW_GROUP_JSON_BASE64_VARIABLE;
import static com.cyoda.presto.client.reporting.metaproviders.StaticReportFields.ROW_REPORT_ID_COLUMN;
import static com.cyoda.presto.client.reporting.metaproviders.StaticReportFields.ROW_REPORT_ROW_NUMBER_COLUMN;

public class DynamicTableDataProvider extends TableDataProvider<RowHandle> {
    private final CyodaTableHandle tableHandle;
    private final ReportHistoryApiHandler reportHistoryApiHandler;
    private final ReportGroupsApiHandler reportGroupsApiHandler;
    private final ReportRowsApiHandler reportRowsApiHandler;

    public DynamicTableDataProvider(CyodaTableHandle tableHandle,
                                    ReportHistoryApiHandler reportHistoryApiHandler,
                                    ReportGroupsApiHandler reportGroupsApiHandler,
                                    ReportRowsApiHandler reportRowsApiHandler) {
        this.tableHandle = tableHandle;
        this.reportHistoryApiHandler = reportHistoryApiHandler;
        this.reportGroupsApiHandler = reportGroupsApiHandler;
        this.reportRowsApiHandler = reportRowsApiHandler;
    }

    @Override
    public Iterable<RowHandle> getIterable(AuthContext authContext, CyodaTableHandle tableHandle, CompoundPredicateNode predicates) {
        //TODO return iterable should be from flux
        //TODO add predicate optimizations
        return reportHistoryApiHandler.getByKey(tableHandle.getReportConfigId())
                .stream()
                .flatMap(fieldsView ->
                        reportGroupsApiHandler.getByKey(
                                new GroupsRequestKey(fieldsView.getReportId(),
                                        fieldsView.getGroupingVersion())
                        ).stream())
                .flatMap(groupingHandle ->
                        reportRowsApiHandler
                                .asFlux(RowsRequestKey.of(groupingHandle),
                                        predicates,
                                        SizeListener.NOT_LISTENING)
                                .subscribeOn(Schedulers.parallel())  // Probably the default.
                                .toStream()
                        ).collect(Collectors.toList());
    }

    @Nullable
    @Override
    protected Object getFieldValueFromEntity(@Nonnull RowHandle field, CyodaColumnHandle columnHandle) {
        if (ROW_REPORT_ROW_NUMBER_COLUMN.equals(columnHandle.getColumnName())) {
            return field.rowNum();
        }
        if (ROW_REPORT_ID_COLUMN.equals(columnHandle.getColumnName())) {
            return field.reportId();
        }
        if (ROW_GROUPING_VERSION_COLUMN.equals(columnHandle.getColumnName())) {
            return field.groupingVersion();
        }
        if (ROW_GROUP_JSON_BASE64_VARIABLE.equals(columnHandle.getColumnName())) {
            return field.groupJsonBase64();
        }
        return field.reportRow().get(columnHandle.getColumnName());
    }

}
