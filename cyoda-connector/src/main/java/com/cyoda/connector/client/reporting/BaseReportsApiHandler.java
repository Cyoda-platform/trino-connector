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

package com.cyoda.connector.client.reporting;

import com.cyoda.connector.CyodaConfig;
import com.cyoda.connector.SizeListener;
import com.cyoda.connector.auth.AuthService;
import com.cyoda.connector.client.RestTemplateCustomizer;
import com.cyoda.connector.client.logic.CompoundPredicateNode;
import com.cyoda.connector.client.reporting.stats.CyodaApiRequestStatsMonitor;
import com.cyoda.connector.logging.SupplierLogger;
import com.google.common.base.Preconditions;
import io.trino.spi.StandardErrorCode;
import io.trino.spi.TrinoException;
import io.trino.spi.connector.SchemaTableName;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Nonnull;
import java.net.URI;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static com.cyoda.connector.CyodaErrorCode.CYODA_API_ERROR;
import static com.cyoda.connector.CyodaErrorCode.CYODA_TOO_MANY_REQUESTS;
import static com.google.common.base.MoreObjects.toStringHelper;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

// TODO: The API calls to Cyoda need to have some check on API version. Sasha might be able to say how he did it for UI
public abstract class BaseReportsApiHandler {

    public static final String REPORT_ENDPOINT = "/api/platform-api/reporting/report";
    public static final String PAGE_REQUEST_PARAMETER = "page";
    public static final String SIZE_REQUEST_PARAMETER = "size";
    public static final String FIELDS_REQUEST_PARAMETER = "fields";

    public static final int DEFAULT_PAGE_SIZE = 10;

    protected final CyodaConfig config;
    protected final RestTemplateCustomizer restTemplateCustomizer;
    protected final SupplierLogger log;
    protected final AuthService auth;

    private final CyodaApiRequestStatsMonitor requestStatsMonitor;

    protected BaseReportsApiHandler(CyodaConfig config,
                                    RestTemplateCustomizer restTemplateCustomizer,
                                    SupplierLogger log,
                                    AuthService authService,
                                    CyodaApiRequestStatsMonitor requestStatsMonitor) {
        this.config = requireNonNull(config, "config is null");
        this.restTemplateCustomizer = restTemplateCustomizer;
        this.log = log;
        this.auth = authService;
        this.requestStatsMonitor = requestStatsMonitor;
    }

    @SuppressWarnings("SameParameterValue")
    public RuntimeException requestFailedException(Object me, HttpClientErrorException e, URI uri) {
        if (HttpStatus.UNAUTHORIZED.equals(e.getStatusCode())) {
            return new TrinoException(StandardErrorCode.PERMISSION_DENIED, "Authentication failed : " + e.getStatusText());
        }
        if (HttpStatus.TOO_MANY_REQUESTS.equals(e.getStatusCode())) {
            return new TrinoException(CYODA_TOO_MANY_REQUESTS, "Request throttled : " + e.getStatusText());
        }

        return new TrinoException(CYODA_API_ERROR,
                format("[Cyoda] Error %s at %s returned an invalid response: %s [Error: %s]",
                        getName(), uri.toASCIIString(), asString(me,e), e.getResponseBodyAsString()),
                e
        );
    }

    private static String asString(Object me, HttpClientErrorException e) {
        return toStringHelper(me)
                .add("statusCode", e.getStatusCode())
                .add("statusMessage", e.getStatusText())
                .add("headers", e.getResponseHeaders())
                .omitNullValues()
                .toString();
    }

    protected void registerApiCall(String queryId, Date callTime, String requestUrl, Map<String, Object> params){
        requestStatsMonitor.registerApiCall(queryId, callTime, requestUrl, params, getName());
    }
    public String getName(){
        return this.getClass().getSimpleName().replaceAll("ApiHandler", "");
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
    public static @Nonnull SchemaTableName configIdToSchemaTableName(@Nonnull String reportConfigId){
        int ownerIndex = reportConfigId.indexOf("-");
        if (ownerIndex == -1) {
            return new SchemaTableName("reports_owner_unknown", reportConfigId.toLowerCase());
        } else {
            return new SchemaTableName("reports_" + reportConfigId.substring(0, ownerIndex).toLowerCase(),
                    reportConfigId.substring(ownerIndex + 1));
        }
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

    protected RestTemplate getTechRestTemplate() {
        return restTemplateCustomizer.getRestTemplate(auth.getTechnicalAuth());
    }
}
