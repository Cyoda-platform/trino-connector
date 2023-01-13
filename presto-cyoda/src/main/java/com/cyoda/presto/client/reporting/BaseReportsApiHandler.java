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
import com.cyoda.presto.auth.AuthService;
import com.cyoda.presto.client.RestTemplateCustomizer;
import com.cyoda.presto.client.logic.CompoundPredicateNode;
import com.cyoda.presto.client.reporting.stats.ApiRequestStats;
import com.cyoda.presto.client.reporting.stats.CyodaApiRequestStatsMonitor;
import com.cyoda.presto.logging.SupplierLogger;
import com.google.common.base.Preconditions;
import io.trino.spi.type.TypeManager;
import org.springframework.hateoas.PagedModel;

import javax.annotation.Nonnull;
import java.util.Date;
import java.util.Locale;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

// TODO: The API calls to Cyoda need to have some check on API version. Sasha might be able to say how he did it for UI
public abstract class BaseReportsApiHandler<K, T>{

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
    protected final AuthService auth;

    private final CyodaApiRequestStatsMonitor requestStatsMonitor;

    protected BaseReportsApiHandler(CyodaConnectorId connectorId,
                                    CyodaConfig config,
                                    TypeManager typeManager,
                                    RestTemplateCustomizer restTemplateCustomizer,
                                    SupplierLogger log,
                                    AuthService authService,
                                    CyodaApiRequestStatsMonitor requestStatsMonitor) {
        this.connectorId = requireNonNull(connectorId, "connectorId is null");
        this.config = requireNonNull(config, "config is null");
        this.typeManager = requireNonNull(typeManager, "typeManager is null");
        this.restTemplateCustomizer = restTemplateCustomizer;
        this.log = log;
        this.auth = authService;
        this.requestStatsMonitor = requestStatsMonitor;
    }

    protected int getPageSize(){
        return config.getRequestPageSize();
    }

    protected void registerApiCall(String queryId, Date callTime, String requestUrl, Object response){
        requestStatsMonitor.add(
                new ApiRequestStats(
                        queryId,
                        callTime,
                        requestUrl,
                        System.currentTimeMillis() - callTime.getTime(),
                        response));
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

    protected void logCreation(int pageSize, CompoundPredicateNode predicates, SupplierLogger logger) {
        logger.debug("building responseIterator for %s with pageSize %s and predicates %s",
                getClass()::getSimpleName,
                () -> pageSize,
                predicates::toString
        );
    }

    protected void logCreation(int pageSize, SupplierLogger logger) {
        logger.debug("building responseIterator for %s with pageSize %s",
                getClass()::getSimpleName,
                () -> pageSize
        );
    }
}
