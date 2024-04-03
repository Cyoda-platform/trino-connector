package com.cyoda.presto.client.data;

import com.cyoda.core.conditions.GroupCondition;
import com.cyoda.core.conditions.queryable.Equals;
import com.cyoda.core.util.JodaBeanSerUtil;
import com.cyoda.presto.CyodaSplit;
import com.cyoda.presto.auth.AuthContext;
import com.cyoda.presto.client.reporting.stats.CyodaApiRequestStatsMonitor;
import com.cyoda.presto.client.treenode.DomainToCondition;
import com.cyoda.presto.client.treenode.CyodaRSocketClient;
import com.cyoda.presto.client.treenode.dto.DataRequestDto;
import com.cyoda.presto.client.treenode.dto.EntityContentDto;
import com.cyoda.presto.client.types.DataType;
import com.cyoda.presto.handles.CyodaColumnHandle;
import com.cyoda.presto.handles.CyodaTableHandle;
import com.cyoda.presto.handles.CyodaTableMeta;
import com.google.common.collect.Lists;
import io.trino.spi.connector.ColumnHandle;
import io.trino.spi.connector.Constraint;
import io.trino.spi.predicate.Domain;
import io.trino.spi.predicate.EquatableValueSet;
import io.trino.spi.predicate.TupleDomain;
import io.trino.spi.predicate.ValueSet;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;


public class TreeNodeTableDataProvider extends TableDataProvider<EntityContentDto> {

    public static final int BATCH_SIZE = 10;
    private final CyodaApiRequestStatsMonitor apiMonitor;
    private final CyodaRSocketClient client;

    public TreeNodeTableDataProvider(CyodaApiRequestStatsMonitor apiMonitor, CyodaRSocketClient client) {
        this.apiMonitor = apiMonitor;
        this.client = client;
    }


    @Nullable
    @Override
    protected Object getFieldValueFromEntity(@Nonnull EntityContentDto entity, CyodaColumnHandle columnHandle) {
        if ("id".equals(columnHandle.getColumnName())) return entity.getId();
        if ("parent_id".equals(columnHandle.getColumnName())) return entity.getParentId();
        if ("index".equals(columnHandle.getColumnName())) return entity.getIndex();

        if (columnHandle.getDataType().getMainType() == DataType.LIST){
            int count = 0;
            List<Object> result = new ArrayList<>();
            Object currentElement = entity.getContents().get(columnHandle.getColumnKey().replace("*", count++ + ""));
            while (currentElement != null) {
                result.add(currentElement);
                currentElement = entity.getContents().get(columnHandle.getColumnKey().replace("*", count++ + ""));
            }
            return result;
        } else
            return entity.getContents().get(columnHandle.getColumnKey());
    }

    @Override
    public List<CyodaSplit> getSplits(AuthContext authContext, String queryId, CyodaTableHandle tableHandle, Constraint constraint) {
        TupleDomain<ColumnHandle> tupleDomain = tableHandle.getConstraint();
        if (tupleDomain == null || tupleDomain.isAll() || tupleDomain.getDomains().isEmpty())
            return Collections.singletonList(
                    new CyodaSplit(queryId, authContext.getUserId(), tableHandle.getTableMetaId(), null, null, null, tupleDomain));

        Map<ColumnHandle, Domain> columnDomains;
        columnDomains = tupleDomain.getDomains().get();
        Map<ColumnHandle, Integer> constraintSize = new HashMap<>();
        for (Map.Entry<ColumnHandle, Domain> entry : columnDomains.entrySet()) {
            ValueSet valueSet = entry.getValue().getValues();
            if (valueSet.isDiscreteSet() && valueSet instanceof EquatableValueSet) {
                int valuesCount = valueSet.getDiscreteValues().getValuesCount();
                if (valuesCount < BATCH_SIZE) continue;
                constraintSize.put(entry.getKey(), valuesCount);
            }
        }
        if (constraintSize.isEmpty())
            return Collections.singletonList(
                    new CyodaSplit(queryId, authContext.getUserId(), tableHandle.getTableMetaId(), null, null, null, tupleDomain));

        ColumnHandle splitColumn = Collections.max(constraintSize.entrySet(), Map.Entry.comparingByValue()).getKey();
        Domain domain = columnDomains.get(splitColumn);
        List<TupleDomain<ColumnHandle>> replacerDomains = new ArrayList<>();
        List<Object> valueSet = domain.getValues().getDiscreteSet();
        List<List<Object>> partitionedValues = Lists.partition(valueSet, BATCH_SIZE);
        for (List<Object> part : partitionedValues) {
            Domain partDomain = Domain.create(
                    ValueSet.copyOf(domain.getType(), part), domain.isNullAllowed());
            replacerDomains.add(tupleDomain.intersect(TupleDomain.withColumnDomains(Map.of(splitColumn, partDomain))));
        }
        return replacerDomains.stream()
                .map(td -> new CyodaSplit(queryId, authContext.getUserId(), tableHandle.getTableMetaId(), null, null, null, td))
                .toList();
    }


    @Override
    public Iterable<EntityContentDto> getIterable(CyodaTableMeta tableHandle, CyodaSplit split) {
        TupleDomain<ColumnHandle> constraint = split.getConstraint();
        if (constraint.isNone()) return Collections.emptyList();
        String strTableId = split.getCyodaTableMetaId();
        String[] s = strTableId.split("\\|");
        UUID metaClassId = UUID.fromString(s[0]);
        GroupCondition condition;
        String uniformedPath = s[1];
        if (constraint.isAll()) {
            condition = new GroupCondition(GroupCondition.Operator.AND);
        } else {
            condition = DomainToCondition.convert(constraint);
        }
        condition.addCondition(new Equals("entityModelClassId", metaClassId));
        condition.addCondition(new Equals("uniformedPath", uniformedPath));
        String strCondition = JodaBeanSerUtil.compact().jsonWriter().write(condition);
        Iterable<EntityContentDto> result = client.treeNode().getData(new DataRequestDto(metaClassId, split.getUserId(), uniformedPath, strCondition));
        apiMonitor.registerApiCall(split.getQueryId(), new Date(), strTableId, strCondition, "TREE_NODE");
        return result;
    }
}
