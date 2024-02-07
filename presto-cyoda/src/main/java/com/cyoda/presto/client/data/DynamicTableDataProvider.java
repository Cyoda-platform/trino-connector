package com.cyoda.presto.client.data;

import com.cyoda.presto.CyodaCachedPageSource;
import com.cyoda.presto.CyodaConfig;
import com.cyoda.presto.CyodaSplit;
import com.cyoda.presto.auth.AuthContext;
import com.cyoda.presto.client.reporting.data.DataRequestKey;
import com.cyoda.presto.client.reporting.data.ReportRowsApiHandler;
import com.cyoda.presto.client.reporting.data.RowHandle;
import com.cyoda.presto.client.reporting.groups.GroupsRequestKey;
import com.cyoda.presto.client.reporting.groups.ReportGroupsApiHandler;
import com.cyoda.presto.client.reporting.meta.ReportConfigKey;
import com.cyoda.presto.client.reporting.meta.ReportHistoryApiHandler;
import com.cyoda.presto.client.reporting.metaproviders.StaticTableMetadataProvider;
import com.cyoda.presto.client.reporting.stats.ContentIdLoadingCache;
import com.cyoda.presto.client.reporting.stats.CyodaCacheMonitor;
import com.cyoda.presto.handles.CyodaColumnHandle;
import com.cyoda.presto.handles.CyodaTableHandle;
import com.cyoda.presto.handles.CyodaTableMeta;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.trino.spi.connector.ConnectorPageSource;
import io.trino.spi.connector.Constraint;
import io.trino.spi.predicate.TupleDomain;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static com.cyoda.presto.client.reporting.metaproviders.StaticReportFields.ROW_GROUPING_VERSION_COLUMN;
import static com.cyoda.presto.client.reporting.metaproviders.StaticReportFields.ROW_GROUP_JSON_BASE64_VARIABLE;
import static com.cyoda.presto.client.reporting.metaproviders.StaticReportFields.ROW_REPORT_ID_COLUMN;
import static com.cyoda.presto.client.reporting.metaproviders.StaticReportFields.ROW_REPORT_ROW_NUMBER_COLUMN;

public class DynamicTableDataProvider extends TableDataProvider<RowHandle> {
    private final ReportHistoryApiHandler reportHistoryApiHandler;
    private final ReportGroupsApiHandler reportGroupsApiHandler;
    private final ReportRowsApiHandler reportRowsApiHandler;
    private final CyodaConfig config;
    private final CyodaColumnHandle reportIdColumn;
    private final CyodaColumnHandle groupIdColumn;
    private final CyodaColumnHandle rowNumberColumn;

    private final ContentIdLoadingCache<DataRequestKey, CyodaCachedPageSource<RowHandle>> pageCache;

    public DynamicTableDataProvider(ReportHistoryApiHandler reportHistoryApiHandler,
                                    ReportGroupsApiHandler reportGroupsApiHandler,
                                    ReportRowsApiHandler reportRowsApiHandler,
                                    StaticTableMetadataProvider reportMetadataProvider,
                                    CyodaConfig config,
                                    CyodaCacheMonitor cacheMonitor) {
        this.reportHistoryApiHandler = reportHistoryApiHandler;
        this.reportGroupsApiHandler = reportGroupsApiHandler;
        this.reportRowsApiHandler = reportRowsApiHandler;

        reportIdColumn = reportMetadataProvider.getReportRows().getReportIdColumn();
        groupIdColumn = reportMetadataProvider.getReportRows().getGroupJsonBase64Column();
        rowNumberColumn = reportMetadataProvider.getReportRows().getRowNumberColumn();
        this.config = config;
        pageCache =
                new ContentIdLoadingCache<>(Caffeine.newBuilder()
                        .expireAfterAccess(Duration.ofHours(config.getCacheReportPagesHoursAfterAccess()))
                        .recordStats()
                        .build(
                        this::getCachedPageSource
                ));
        cacheMonitor.register("DATA", pageCache,
                key -> key.getReportConfigId() + "|" + key.getReportId() + "|" + key.getGroupJsonBase64() + "|" + key.getPage(),
                page -> (int) page.getCompletedBytes());
    }

    @Override
    public List<CyodaSplit> getSplits(AuthContext authContext, String queryId, CyodaTableHandle tableHandle, Constraint constraint) {
        boolean hasReportIdConstraint = hasConstraint(reportIdColumn, constraint);
        boolean hasGroupIdConstraint = hasConstraint(groupIdColumn, constraint);
        boolean hasRowNumConstraint = hasConstraint(rowNumberColumn, constraint);
        return reportHistoryApiHandler.getByKey(new ReportConfigKey(tableHandle.getTableMetaId(), queryId))
                .stream()
                .filter(fieldsView -> !hasReportIdConstraint || acceptVal(reportIdColumn, fieldsView.getReportId(), constraint))
                .flatMap(fieldsView -> reportGroupsApiHandler.getByKey(
                        new GroupsRequestKey(fieldsView.getReportId(), fieldsView.getGroupingVersion(), queryId)).stream()
                ).filter(groupingHandle -> !hasGroupIdConstraint || acceptVal(
                        groupIdColumn, groupingHandle.groupHeader.getGroupValuesJsonBase64(), constraint)
                )
                .flatMap(groupingHandle -> {
                    int pageSize = config.getRowRequestPageSize();
                    long maxPages = groupingHandle.groupHeader.getRowCount() / pageSize +
                            Long.signum(groupingHandle.groupHeader.getRowCount() % pageSize);
                    return Stream.iterate(0, x -> x < maxPages, x -> x + 1)
                            .filter(page -> !hasRowNumConstraint ||
                                    Stream.iterate(1, x -> x <= pageSize, x -> x + 1)
                                            .map(x -> x + ((long) page * pageSize))
                                            .anyMatch(row -> acceptVal(rowNumberColumn, row, constraint)))
                            .map(page -> new CyodaSplit(
                                    queryId,
                                    new ArrayList<>(),
                                    false,
                                    tableHandle.getTableMetaId(),
                                    groupingHandle.reportId,
                                    groupingHandle.groupingVersion,
                                    groupingHandle.groupHeader.getGroupValuesJsonBase64(),
                                    page, pageSize, null, TupleDomain.all()));
                })
                .toList();
    }

    @Override //this is used only while loading an uncached page
    public Iterable<RowHandle> getIterable(CyodaTableMeta tableHandle, CyodaSplit split) {
        return reportRowsApiHandler.getIterable(split);
    }

    @Override //this is using cached pages
    public ConnectorPageSource getPageSource(CyodaTableMeta tableHandle, List<CyodaColumnHandle> cyodaColumns, CyodaSplit split) {
        return pageCache.get(new DataRequestKey(split, tableHandle)).mapNewPage(cyodaColumns);
    }

    @Nonnull
    public CyodaCachedPageSource<RowHandle> getCachedPageSource(DataRequestKey requestKey) {
        return new CyodaCachedPageSource<>(this, requestKey.getTableHandle(), requestKey.getSplit());
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
