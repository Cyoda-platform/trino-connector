/*
 * Copyright (C) 2022 Cyoda Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.cyoda.presto.client.reporting;

import com.cyoda.presto.CyodaConfig;
import com.cyoda.presto.CyodaConnectorId;
import com.cyoda.presto.SizeListener;
import com.cyoda.presto.client.ApiRequestHandler;
import com.cyoda.presto.client.RestTemplateCustomizer;
import com.cyoda.presto.client.logic.CompoundPredicateNode;
import com.cyoda.presto.handles.CyodaColumnHandle;
import com.cyoda.presto.handles.CyodaTableHandle;
import com.cyoda.presto.logging.SupplierLogger;
import com.google.common.base.Preconditions;
import io.trino.spi.block.BlockBuilder;
import io.trino.spi.type.TypeManager;
import org.springframework.hateoas.PagedModel;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Locale;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

// TODO: The API calls to Cyoda need to have some check on API version. Sasha might be able to say how he did it for UI
public abstract class BaseReportsApiHandler<T> implements ApiRequestHandler<T> {

    public static final String REPORT_ENDPOINT = "/api/platform-api/reporting/report";
    public static final String PAGE_REQUEST_PARAMETER = "page";
    public static final String SIZE_REQUEST_PARAMETER = "size";
    public static final String FIELDS_REQUEST_PARAMETER = "fields";

    public static final int DEFAULT_PAGE_SIZE = 10;

    protected final CyodaConnectorId connectorId;
    protected final CyodaConfig config;
    protected final RestTemplateCustomizer restTemplateCustomizer;
    protected final TypeManager typeManager;
    protected final SupplierLogger log;

    protected BaseReportsApiHandler(CyodaConnectorId connectorId,
                                    CyodaConfig config,
                                    TypeManager typeManager,
                                    RestTemplateCustomizer restTemplateCustomizer,
                                    SupplierLogger log) {
        this.connectorId = requireNonNull(connectorId, "connectorId is null");
        this.config = requireNonNull(config, "config is null");
        this.typeManager = requireNonNull(typeManager, "typeManager is null");
        this.restTemplateCustomizer = restTemplateCustomizer;
        this.log = log;
    }

    @Override
    public void writeValue(@Nullable T entity, CyodaColumnHandle columnHandle, BlockBuilder blockBuilder) {
        if (entity == null) {
            blockBuilder.appendNull();
            return;
        }
        Object value = getFieldValueFromEntity(entity, columnHandle);
        columnHandle.writeValue(blockBuilder, value);
    }

    protected abstract @Nullable Object getFieldValueFromEntity(@Nonnull T field, CyodaColumnHandle columnHandle);

    protected int getPageSize(){
        return config.getRequestPageSize();
    }

    protected static String toReportName(@Nonnull String reportConfigId) {
        Preconditions.checkNotNull(reportConfigId, "reportConfigId is null");
        int start = reportConfigId.lastIndexOf('-');
        if (start < 0) {
            throw new IllegalArgumentException("report ID " + reportConfigId + " has incompatible format." +
                    " It should be <Tenant>-<EntityTypee>-<ReportName>");
        }
        String reportName = reportConfigId.substring(start + 1);
        Preconditions.checkArgument(!reportName.isEmpty(), "report ID '%s' has incompatible format." +
                " It should be <Tenant>-<EntityTypee>-<ReportName>", reportConfigId);
        return reportName;
    }

    public static @Nonnull String reportNameToTableName(@Nonnull String reportName) {
        Preconditions.checkNotNull(reportName, "reportName is null");
        Preconditions.checkArgument(!reportName.isEmpty(), "reportName is empty");
        String result = reportName
                //.replace(" ", "")
                //.replaceAll("[$\\-&%§@*#, ']", "_") // Let's not allow complicated things.
                //.replaceAll("([a-z])([A-Z]+)", "$1_$2")
                .toLowerCase(Locale.ROOT);
        Preconditions.checkArgument(!result.isEmpty(), "generated tableName is empty");
        return result;
    }

    protected <S> void publishSize(SizeListener listener, PagedModel<S> pagedModel) {
        PagedModel.PageMetadata pageMetadata = Optional.ofNullable(pagedModel)
                .map(PagedModel::getMetadata)
                .orElse(PagedModel.empty().getMetadata());
        listener.sizeKnown(pageMetadata == null ? 0 : pageMetadata.getTotalElements());
    }

    protected static void logCreation(int pageSize, CyodaTableHandle tableHandle, CompoundPredicateNode predicates, SupplierLogger logger) {
        logger.debug("building responseIterator for %s with pageSize %s and predicates %s",
                tableHandle::getTableName,
                () -> pageSize,
                predicates::toString
        );
    }

}
