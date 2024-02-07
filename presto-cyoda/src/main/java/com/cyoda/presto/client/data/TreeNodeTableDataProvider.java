package com.cyoda.presto.client.data;

import com.cyoda.core.conditions.GroupCondition;
import com.cyoda.core.util.JodaBeanSerUtil;
import com.cyoda.presto.CyodaSplit;
import com.cyoda.presto.auth.AuthContext;
import com.cyoda.presto.client.reporting.stats.CyodaApiRequestStatsMonitor;
import com.cyoda.presto.client.treenode.DomainToCondition;
import com.cyoda.presto.client.treenode.TreeNodeTrinoAPIMock;
import com.cyoda.presto.client.treenode.dto.EntityContentDto;
import com.cyoda.presto.handles.CyodaColumnHandle;
import com.cyoda.presto.handles.CyodaTableHandle;
import com.cyoda.presto.handles.CyodaTableMeta;
import io.trino.spi.connector.ColumnHandle;
import io.trino.spi.connector.Constraint;
import io.trino.spi.predicate.TupleDomain;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;


public class TreeNodeTableDataProvider extends TableDataProvider<EntityContentDto> {

    private final TreeNodeTrinoAPIMock treeNodeApi = new TreeNodeTrinoAPIMock();
    private final CyodaApiRequestStatsMonitor apiMonitor;

    public TreeNodeTableDataProvider(CyodaApiRequestStatsMonitor apiMonitor) {
        this.apiMonitor = apiMonitor;
    }


    @Nullable
    @Override
    protected Object getFieldValueFromEntity(@Nonnull EntityContentDto entity, CyodaColumnHandle columnHandle) {
        if ("id".equals(columnHandle.getExternalName())) return entity.getId();
        if ("parent_id".equals(columnHandle.getExternalName())) return entity.getParentId();

        return entity.getContents().get(columnHandle.getExternalName());
    }

    @Override
    public List<CyodaSplit> getSplits(AuthContext authContext, String queryId, CyodaTableHandle tableHandle, Constraint constraint) {
        return Collections.singletonList(
                new CyodaSplit(queryId, tableHandle.getTableMetaId(), null, null, null, tableHandle.getConstraint()));
    }

    @Override
    public Iterable<EntityContentDto> getIterable(CyodaTableMeta tableHandle, CyodaSplit split) {
        TupleDomain<ColumnHandle> constraint = split.getConstraint();
        if (constraint.isNone()) return Collections.emptyList();
        UUID metaClassId = UUID.fromString(split.getCyodaTableMetaId());
        GroupCondition condition = constraint.isAll() ? GroupCondition.ALL : DomainToCondition.convert(constraint);
        String strCondition = JodaBeanSerUtil.pretty().jsonWriter().write(condition);
        apiMonitor.registerApiCall(split.getQueryId(), new Date(), "MOCK["+metaClassId+"]", strCondition, "TREE_NODE");
        return treeNodeApi.getData(metaClassId, condition);
    }
}
