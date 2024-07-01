package com.cyoda.connector.client.data;

import com.cyoda.connector.CyodaSplit;
import com.cyoda.connector.auth.AuthContext;
import com.cyoda.connector.client.reporting.groups.GroupingHandle;
import com.cyoda.connector.client.reporting.groups.GroupsRequestKey;
import com.cyoda.connector.client.reporting.groups.ReportGroupsApi;
import com.cyoda.connector.client.reporting.meta.ReportConfigKey;
import com.cyoda.connector.client.reporting.meta.ReportHistoryApi;
import com.cyoda.connector.client.reporting.metaproviders.StaticTableMetadataProvider;
import com.cyoda.connector.handles.CyodaColumnHandle;
import com.cyoda.connector.handles.CyodaTableHandle;
import com.cyoda.connector.handles.CyodaTableMeta;
import io.trino.spi.connector.Constraint;
import org.joda.beans.MetaProperty;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.List;

import static com.cyoda.connector.client.reporting.metaproviders.StaticReportFields.HISTORY_REPORT_ID_COLUMN;
import static com.cyoda.connector.client.reporting.metaproviders.StaticReportFields.GROUPING_VERSION_COLUMN;

public class GroupsTableDataProvider extends TableDataProvider<GroupingHandle> {

    private final ReportHistoryApi reportHistoryApiHandler;
    private final ReportGroupsApi reportGroupsApiHandler;

    private final CyodaColumnHandle reportIdColumn;


    public GroupsTableDataProvider(ReportHistoryApi reportHistoryApiHandler,
                                   ReportGroupsApi reportGroupsApiHandler,
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
