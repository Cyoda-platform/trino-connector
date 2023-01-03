package com.cyoda.presto.client.data;

import com.cyoda.presto.SizeListener;
import com.cyoda.presto.auth.AuthContext;
import com.cyoda.presto.client.reporting.data.ReportRowsApiHandler;
import com.cyoda.presto.client.reporting.data.RowHandle;
import com.cyoda.presto.client.reporting.data.RowsRequestKey;
import com.cyoda.presto.client.reporting.groups.GroupingHandle;
import com.cyoda.presto.client.reporting.groups.GroupsRequestKey;
import com.cyoda.presto.client.reporting.groups.ReportGroupsApiHandler;
import com.cyoda.presto.client.reporting.meta.ReportHistoryApiHandler;
import com.cyoda.presto.client.reporting.metaproviders.StaticReportMetadataProvider;
import com.cyoda.presto.handles.CyodaColumnHandle;
import com.cyoda.presto.handles.CyodaTableHandle;
import io.trino.spi.connector.Constraint;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

import static com.cyoda.presto.client.reporting.metaproviders.StaticReportFields.ROW_GROUPING_VERSION_COLUMN;
import static com.cyoda.presto.client.reporting.metaproviders.StaticReportFields.ROW_GROUP_JSON_BASE64_VARIABLE;
import static com.cyoda.presto.client.reporting.metaproviders.StaticReportFields.ROW_REPORT_ID_COLUMN;
import static com.cyoda.presto.client.reporting.metaproviders.StaticReportFields.ROW_REPORT_ROW_NUMBER_COLUMN;

public class DynamicTableDataProvider extends TableDataProvider<RowHandle> {
    private final ReportHistoryApiHandler reportHistoryApiHandler;
    private final ReportGroupsApiHandler reportGroupsApiHandler;
    private final ReportRowsApiHandler reportRowsApiHandler;
    private final StaticReportMetadataProvider reportMetadataProvider;

    public DynamicTableDataProvider(ReportHistoryApiHandler reportHistoryApiHandler,
                                    ReportGroupsApiHandler reportGroupsApiHandler,
                                    ReportRowsApiHandler reportRowsApiHandler,
                                    StaticReportMetadataProvider reportMetadataProvider) {
        this.reportHistoryApiHandler = reportHistoryApiHandler;
        this.reportGroupsApiHandler = reportGroupsApiHandler;
        this.reportRowsApiHandler = reportRowsApiHandler;
        this.reportMetadataProvider = reportMetadataProvider;
    }

    @Override
    public Iterable<RowHandle> getIterable(AuthContext authContext, CyodaTableHandle tableHandle, Constraint constraint) {
//        CyodaColumnHandle reportIdColumn = reportMetadataProvider.getReportRows().getReportIdColumn();
//        PredicateTraversal<String> traversal = PredicateTraversal.of(predicates, String.class);
//        Set<ColumnPredicate<String>> reportIdParsed = traversal.parseFor(reportIdColumn);
//        CyodaColumnHandle groupIdColumn = reportMetadataProvider.getReportRows().getGroupJsonBase64Column();
//        Set<ColumnPredicate<String>> groupIdParsed = traversal.parseFor(groupIdColumn);
        List<GroupingHandle> groupList = reportHistoryApiHandler.getByKey(tableHandle.getReportConfigId())
                .stream()
//                .filter(fieldsView -> acceptValue(reportIdColumn, fieldsView.getReportId(), reportIdParsed))
                .flatMap(fieldsView -> reportGroupsApiHandler.getByKey(
                        new GroupsRequestKey(fieldsView.getReportId(), fieldsView.getGroupingVersion())).stream()
//                ).filter(groupingHandle -> acceptValue(
//                        groupIdColumn, groupingHandle.groupHeader.getGroupValuesJsonBase64(), groupIdParsed)
                ).toList();

        return Flux.fromIterable(groupList)
                .flatMap(groupingHandle ->
                        reportRowsApiHandler
                                .asFlux(RowsRequestKey.of(groupingHandle),
                                        constraint, //predicates,
                                        SizeListener.NOT_LISTENING)

                        )
                .subscribeOn(Schedulers.parallel())  // Probably the default.
                .toIterable();
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
        return columnHandle.getValue(field.reportRow());
    }

}
