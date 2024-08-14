package com.cyoda.connector.client.data;

import com.cyoda.connector.CyodaSplit;
import com.cyoda.connector.auth.AuthContext;
import com.cyoda.connector.client.reporting.metaproviders.StaticTableMetadata;
import com.cyoda.connector.client.reporting.metaproviders.StaticTableMetadata.RawEntityContentColumnDef;
import com.cyoda.connector.client.treenode.CyodaRSocketClient;
import com.cyoda.connector.client.treenode.DomainToCondition;
import com.cyoda.connector.client.treenode.dto.DataRequestDto;
import com.cyoda.connector.client.treenode.dto.EntityContentDto;
import com.cyoda.connector.client.treenode.dto.RawEntityContentDto;
import com.cyoda.connector.client.types.DataType;
import com.cyoda.connector.handles.CyodaColumnHandle;
import com.cyoda.connector.handles.CyodaTableHandle;
import com.cyoda.connector.handles.CyodaTableMeta;
import com.cyoda.core.conditions.GroupCondition;
import com.cyoda.core.conditions.queryable.Equals;
import com.cyoda.core.util.JodaBeanSerUtil;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;


public class RawTreeNodeDataProvider extends TableDataProvider<RawEntityContentDto> {


    private final CyodaRSocketClient client;

    public RawTreeNodeDataProvider(CyodaRSocketClient client) {
        this.client = client;
    }


    @Nullable
    @Override
    protected Object getFieldValueFromEntity(@Nonnull RawEntityContentDto entity, CyodaColumnHandle columnHandle) {
        RawEntityContentColumnDef staticColumn = RawEntityContentColumnDef.getByFieldName(columnHandle.getColumnName());
        switch (staticColumn) {
            case ID -> {
                return entity.getId();
            }
            case ENTITY_MODEL_CLASS_ID -> {
                return entity.getEntityModelClassId();
            }
            case ENTITY_MODEL_NAME -> {
                return entity.getEntityModelName();
            }
            case ENTITY_MODEL_VERSION -> {
                return entity.getEntityModelVersion();
            }
            case ROOT_ID -> {
                return entity.getRootId();
            }
            case PARENT_ID -> {
                return entity.getParentId();
            }
            case PATH -> {
                return entity.getPath();
            }
            case DEPTH -> {
                return entity.getDepth();
            }
            case INDEX -> {
                return entity.getIndex();
            }
            case UNIFORMED_PATH -> {
                return entity.getUniformedPath();
            }
            case LAST_UPDATE_DATE -> {
                return entity.getLastUpdateDate();
            }
            case CONTENTS -> {
                return entity.getContents();
            }
            case TYPE_REFERENCE -> {
                return entity.getTypeReference();
            }
            default -> throw new RuntimeException("Unknown column: " + staticColumn);
        }
    }

    @Override
    public List<CyodaSplit> getSplits(AuthContext authContext, String queryId, CyodaTableHandle tableHandle, Constraint constraint) {
        return List.of(new CyodaSplit(queryId, authContext.getUserId(), tableHandle));
    }


    @Override
    public Iterable<RawEntityContentDto> getIterable(CyodaTableMeta tableHandle, CyodaSplit split) {
        Iterable<RawEntityContentDto> result = client.treeNodeClient.rawDataRequester.retrieveData(split.getQueryId(), split.getUserId()).toIterable();
        return result;
    }
}
