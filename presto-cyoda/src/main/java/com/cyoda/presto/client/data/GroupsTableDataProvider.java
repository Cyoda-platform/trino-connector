package com.cyoda.presto.client.data;

import com.cyoda.presto.auth.AuthContext;
import com.cyoda.presto.client.reporting.groups.GroupingHandle;
import com.cyoda.presto.client.reporting.groups.GroupsRequestKey;
import com.cyoda.presto.client.reporting.groups.ReportGroupsApiHandler;
import com.cyoda.presto.client.reporting.meta.ReportHistoryApiHandler;
import com.cyoda.presto.client.reporting.metaproviders.StaticReportMetadataProvider;
import com.cyoda.presto.handles.CyodaColumnHandle;
import com.cyoda.presto.handles.CyodaTableHandle;
import io.trino.spi.connector.Constraint;
import org.joda.beans.MetaProperty;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.stream.Collectors;

import static com.cyoda.presto.client.reporting.metaproviders.StaticReportFields.HISTORY_REPORT_ID_COLUMN;
import static com.cyoda.presto.client.reporting.metaproviders.StaticReportFields.GROUPING_VERSION_COLUMN;

public class GroupsTableDataProvider extends TableDataProvider<GroupingHandle> {

    private final ReportHistoryApiHandler reportHistoryApiHandler;
    private final ReportGroupsApiHandler reportGroupsApiHandler;
    private final StaticReportMetadataProvider reportMetadataProvider;


    public GroupsTableDataProvider(ReportHistoryApiHandler reportHistoryApiHandler,
                                   ReportGroupsApiHandler reportGroupsApiHandler,
                                   StaticReportMetadataProvider reportMetadataProvider) {
        this.reportHistoryApiHandler = reportHistoryApiHandler;
        this.reportGroupsApiHandler = reportGroupsApiHandler;
        this.reportMetadataProvider = reportMetadataProvider;
    }


    @Nullable
    @Override
    protected Object getFieldValueFromEntity(@Nonnull GroupingHandle field, CyodaColumnHandle columnHandle) {
        if (HISTORY_REPORT_ID_COLUMN.equals(columnHandle.getColumnName())) {
            return field.reportId;
        }
        if (GROUPING_VERSION_COLUMN.equals(columnHandle.getColumnName())) {
            return field.groupingVersion;
        }
        MetaProperty<?> metaProperty = field.groupHeader.metaBean().metaPropertyMap().get(columnHandle.getColumnName());
        if (metaProperty == null) {
            throw new IllegalArgumentException(columnHandle.getColumnName() + " is not defined on ReportDefinitionsView");
        }
        return field.groupHeader.metaBean().metaProperty(columnHandle.getColumnName()).get(field.groupHeader);
    }

    @Override
    public Iterable<GroupingHandle> getIterable(AuthContext authContext, CyodaTableHandle tableHandle, Constraint constraint) {
//        CyodaColumnHandle reportIdColumn = reportMetadataProvider.getReportRows().getReportIdColumn();
//        PredicateTraversal<String> traversal = PredicateTraversal.of(predicates, String.class);
//        Set<ColumnPredicate<String>> reportIdParsed = traversal.parseFor(reportIdColumn);
        return reportHistoryApiHandler.getByKey(tableHandle.getReportConfigId())
                .stream()
//                .filter(fieldsView -> acceptValue(reportIdColumn, fieldsView.getReportId(), reportIdParsed))
                .flatMap(fieldsView ->
                        reportGroupsApiHandler.getByKey(
                                new GroupsRequestKey(fieldsView.getReportId(),
                                        fieldsView.getGroupingVersion())
                        ).stream()).collect(Collectors.toList());
    }

}
