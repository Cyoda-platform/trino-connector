package com.cyoda.connector.client.data;

import com.cyoda.core.conditions.GroupCondition;
import com.cyoda.core.conditions.queryable.Equals;
import com.cyoda.core.util.JodaBeanSerUtil;
import com.cyoda.connector.CyodaSplit;
import com.cyoda.connector.auth.AuthContext;
import com.cyoda.connector.client.treenode.DomainToCondition;
import com.cyoda.connector.client.treenode.CyodaRSocketClient;
import com.cyoda.connector.client.treenode.dto.DataRequestDto;
import com.cyoda.connector.client.treenode.dto.EntityContentDto;
import com.cyoda.connector.client.types.DataType;
import com.cyoda.connector.handles.CyodaColumnHandle;
import com.cyoda.connector.handles.CyodaTableHandle;
import com.cyoda.connector.handles.CyodaTableMeta;
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
    private final CyodaRSocketClient client;

    public TreeNodeTableDataProvider(CyodaRSocketClient client) {
        this.client = client;
    }


    @Nullable
    @Override
    protected Object getFieldValueFromEntity(@Nonnull EntityContentDto entity, CyodaColumnHandle columnHandle) {
        if ("id".equals(columnHandle.getColumnName())) return entity.getId();
        if ("point_time".equals(columnHandle.getColumnName())) return entity.getPointTime();

        if (columnHandle.getColumnKey() == null) return switch (columnHandle.getExternalName()) {
            case "id" -> entity.getRootId();
            case "parent" -> entity.getParentId();
            case "index" -> entity.getIndex();
            case "creationDate" -> entity.getCreateDate();
            case "lastUpdateTime" -> entity.getLastUpdateDate();
            default -> throw new RuntimeException("Unrecognized static field " + columnHandle.getExternalName());
        };

        if (columnHandle.getDataType().getMainType() == DataType.LIST){
            int count = 0;
            List<Object> result = new ArrayList<>();
            do {
                Object currentElement = entity.getContents().get(columnHandle.getColumnKey() + "[" + count++ + "]");
                if (currentElement == null) break;
                result.add(currentElement);
            } while (true);
            return result;
        } else
            return entity.getContents().get(columnHandle.getColumnKey());
    }

    @Override
    public List<CyodaSplit> getSplits(AuthContext authContext, String queryId, CyodaTableHandle tableHandle, Constraint constraint) {
        TupleDomain<ColumnHandle> tupleDomain = tableHandle.getConstraint();
        if (tupleDomain == null || tupleDomain.isAll() || tupleDomain.getDomains().isEmpty())
            return Collections.singletonList(
                    new CyodaSplit(queryId, authContext.getUserId(), tableHandle));

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
                    new CyodaSplit(queryId, authContext.getUserId(), tableHandle));

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
                .map(td -> new CyodaSplit(queryId, authContext.getUserId(), tableHandle.withConstraint(td)))
                .toList();
    }


    @Override
    public Iterable<EntityContentDto> getIterable(CyodaTableMeta tableHandle, CyodaSplit split) {
        TupleDomain<ColumnHandle> constraint = split.getTableHandle().getConstraint();
        if (constraint.isNone()) return Collections.emptyList();
        String strTableId = split.getTableHandle().getTableMetaId();
        String[] s = strTableId.split("\\|");
        UUID metaClassId = UUID.fromString(s[0]);
        GroupCondition condition;
        String uniformedPath = s[1];
        Date pointTime = null;
        if (constraint.isAll()) {
            condition = new GroupCondition(GroupCondition.Operator.AND);
        } else {
            CyodaColumnHandle pointTimeColumn = tableHandle.getColumn("point_time");
            if (pointTimeColumn != null)
                pointTime = (Date)DomainToCondition.extractSingleEquals(constraint, pointTimeColumn);
            condition = DomainToCondition.convert(constraint, "point_time");
        }

        String strCondition = JodaBeanSerUtil.compact().jsonWriter().write(condition);
        String queryId = split.getQueryId();
        DataRequestDto dataRequest = new DataRequestDto(
                metaClassId,
                split.getUserId(),
                uniformedPath,
                strCondition,
                pointTime,
                split.getTableHandle().getSelectedFields(),
                split.getTableHandle().getSortingFields(),
                split.getTableHandle().getLimit());
        return client.treeNodeClient.dataRequester.retrieveData(queryId, dataRequest).toIterable();
    }
}
