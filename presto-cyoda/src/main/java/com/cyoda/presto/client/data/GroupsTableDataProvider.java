package com.cyoda.presto.client.data;

import com.cyoda.presto.CyodaSplit;
import com.cyoda.presto.auth.AuthContext;
import com.cyoda.presto.client.reporting.groups.GroupingHandle;
import com.cyoda.presto.client.reporting.groups.GroupsRequestKey;
import com.cyoda.presto.client.reporting.groups.ReportGroupsApiHandler;
import com.cyoda.presto.client.reporting.meta.ReportConfigKey;
import com.cyoda.presto.client.reporting.meta.ReportHistoryApiHandler;
import com.cyoda.presto.client.reporting.metaproviders.StaticTableMetadataProvider;
import com.cyoda.presto.handles.CyodaColumnHandle;
import com.cyoda.presto.handles.CyodaTableHandle;
import com.cyoda.presto.handles.CyodaTableMeta;
import io.trino.spi.connector.Constraint;
import org.joda.beans.MetaProperty;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.List;

import static com.cyoda.presto.client.reporting.metaproviders.StaticReportFields.HISTORY_REPORT_ID_COLUMN;
import static com.cyoda.presto.client.reporting.metaproviders.StaticReportFields.GROUPING_VERSION_COLUMN;

public class GroupsTableDataProvider extends TableDataProvider<GroupingHandle> {

    private final ReportHistoryApiHandler reportHistoryApiHandler;
    private final ReportGroupsApiHandler reportGroupsApiHandler;

    private final CyodaColumnHandle reportIdColumn;


    public GroupsTableDataProvider(ReportHistoryApiHandler reportHistoryApiHandler,
                                   ReportGroupsApiHandler reportGroupsApiHandler,
                                   StaticTableMetadataProvider reportMetadataProvider) {
        this.reportHistoryApiHandler = reportHistoryApiHandler;
        this.reportGroupsApiHandler = reportGroupsApiHandler;
        reportIdColumn = reportMetadataProvider.getReportGroups().getReportIdColumn();
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
    public List<CyodaSplit> getSplits(AuthContext authContext, String queryId, CyodaTableHandle tableHandle, Constraint constraint) {
        boolean hasReportIdConstraint = hasConstraint(reportIdColumn, constraint);
        return reportHistoryApiHandler.getByKey(new ReportConfigKey(tableHandle.getTableMetaId(), queryId))
                .stream()
                .filter(fieldsView -> !hasReportIdConstraint || acceptVal(reportIdColumn, fieldsView.getReportId(), constraint))
                .map(fieldsView -> new CyodaSplit(queryId, authContext.getUserId(), tableHandle.getTableMetaId(), fieldsView.getReportId(), fieldsView.getGroupingVersion(), null))
                .toList();
    }

    @Override
    public Iterable<GroupingHandle> getIterable(CyodaTableMeta tableHandle, CyodaSplit split) {
        return reportGroupsApiHandler.getByKey(new GroupsRequestKey(split.getReportId(), split.getGroupingVersion(), split.getQueryId()));
    }

}
