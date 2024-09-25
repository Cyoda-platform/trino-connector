package com.cyoda.connector.client.data;

import com.cyoda.connector.CyodaErrorCode;
import com.cyoda.connector.client.treenode.dto.conditions.AbstractTrinoConditionDto;
import com.cyoda.connector.client.treenode.dto.conditions.GroupTrinoConditionDto;
import com.cyoda.connector.client.treenode.dto.conditions.SimpleTrinoConditionDto;
import com.cyoda.core.conditions.Operation;
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
import io.trino.spi.TrinoException;
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
        switch (columnHandle.getColumnCategory()) {
            case REPORT -> throw new RuntimeException("Report field in TDB");
            case DATA -> {
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
            case ROOT -> {
                return switch (columnHandle.getColumnKey()) {
                    case "creationDate" -> entity.getCreateDate();
                    case "lastUpdateTime" -> entity.getLastUpdateDate();
                    default -> throw new RuntimeException("Unrecognized root field " + columnHandle.getColumnKey());
                };
            }
            case SPECIAL -> {
                return switch (CyodaColumnHandle.SpecialColumn.valueOf(columnHandle.getColumnKey())){
                    case ENTITY_ID -> entity.getRootId();
                    case POINT_TIME -> entity.getPointTime();
                };
            }
            case INDEX -> {
                if (columnHandle.getColumnKey().isEmpty())
                    return entity.getIndex();
                else return entity.getIndex().get(Integer.parseInt(columnHandle.getColumnKey()));
            }

            default -> throw new IllegalStateException("Unexpected value: " + columnHandle.getColumnCategory());
        }
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
        Map<String,Map<String, AbstractTrinoConditionDto>> condition;
        String uniformedPath = s[1];
        if (constraint.isAll()) {
            condition = null;
        } else {
            condition = new HashMap<>();
            Map<ColumnHandle, Domain> domainMap = constraint.getDomains().get();
            for (Map.Entry<ColumnHandle, Domain> entry : domainMap.entrySet()) {
                Domain domain = entry.getValue();
                CyodaColumnHandle columnHandle = (CyodaColumnHandle) entry.getKey();
                CyodaColumnHandle.ColumnCategory columnCategory = columnHandle.getColumnCategory();
                Map<String, AbstractTrinoConditionDto> categoryMap = condition.computeIfAbsent(columnCategory.toString(), x -> new HashMap<>());
                AbstractTrinoConditionDto trinoCondition = DomainToCondition.createTrinoCondition(columnHandle.getConverter(), domain);
                validatePointTimeCondition(columnCategory, columnHandle, trinoCondition);
                categoryMap.put(columnHandle.getColumnKey(), trinoCondition);
            }
        }

        String queryId = split.getQueryId();
        List<CyodaColumnHandle> selectedFields = split.getTableHandle().getSelectedFields();
        DataRequestDto dataRequest = new DataRequestDto(
                metaClassId,
                split.getUserId(),
                uniformedPath,
                condition,
                selectedFields == null ? null : selectedFields.stream()
                        .filter(f -> f.getColumnCategory() == CyodaColumnHandle.ColumnCategory.DATA)
                        .map(CyodaColumnHandle::getColumnKey).toList());
        return client.treeNodeClient.dataRequester.retrieveData(queryId, dataRequest).toIterable();
    }

    private static void validatePointTimeCondition(CyodaColumnHandle.ColumnCategory columnCategory, CyodaColumnHandle columnHandle, AbstractTrinoConditionDto trinoCondition) {
        if (columnCategory == CyodaColumnHandle.ColumnCategory.SPECIAL &&
                CyodaColumnHandle.SpecialColumn.POINT_TIME.toString().equals(columnHandle.getColumnKey()) &&
        ((trinoCondition instanceof GroupTrinoConditionDto) || ((SimpleTrinoConditionDto) trinoCondition).getOperation() != Operation.EQUALS)
            ){
            String columnName = columnHandle.getColumnName();
            throw new TrinoException(CyodaErrorCode.CYODA_UNSUPPORTED_CONDITION, "Only single value conditions are allowed for field " + columnName);
        }
    }
}
