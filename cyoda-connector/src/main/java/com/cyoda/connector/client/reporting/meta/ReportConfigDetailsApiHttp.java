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

package com.cyoda.connector.client.reporting.meta;

import com.cyoda.connector.CyodaConfig;
import com.cyoda.connector.CyodaConnectorId;
import com.cyoda.connector.auth.AuthService;
import com.cyoda.connector.client.RestTemplateCustomizer;
import com.cyoda.connector.client.reporting.stats.CyodaApiRequestStatsMonitor;
import com.cyoda.connector.logging.SupplierLogger;
import com.google.common.collect.ImmutableList;
import io.trino.spi.StandardErrorCode;
import io.trino.spi.TrinoException;
import io.trino.spi.type.TypeManager;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.TemplateVariable;
import org.springframework.hateoas.TemplateVariables;
import org.springframework.hateoas.UriTemplate;
import org.springframework.hateoas.client.Traverson;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import jakarta.inject.Inject;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

import static com.cyoda.connector.CyodaErrorCode.CYODA_API_ERROR;
import static com.cyoda.connector.CyodaErrorCode.CYODA_TOO_MANY_REQUESTS;
import static com.cyoda.connector.client.reporting.meta.ConfiguredReportsApiHttp.REPORT_DEFS_ENDPOINT;
import static com.cyoda.connector.client.reporting.meta.ReportDefinitionHandle.REPORT_ID_COLUMN;
import static com.google.common.base.MoreObjects.toStringHelper;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

// TODO: Need to have a plan/solution for report configurations that have changed, and for which existing reports
// exist (with the old version). Maybe we should have a design (in Cyoda) that assembles possible report configurations
// from report histories, and generates the reports table from that. Or better yet, have an API endpoint that
// returns "all" report configurations, existing ones and ones that are stored with a report, in an aggregated fashion
public class ReportConfigDetailsApiHttp implements ReportConfigDetailsApi {

    public static final String REPORT_ENDPOINT = "/api/platform-api/reporting/report";
    protected static final SupplierLogger LOG = SupplierLogger.get(ReportConfigDetailsApiHttp.class);

    public static final String REPORT_DETAILS_ENDPOINT = REPORT_DEFS_ENDPOINT + "/";

    // These are also reserved words for column names coming from reports.
    // TODO This validation needs to be moved to platform

    protected final TypeManager typeManager;
    protected final CyodaConfig config;
    protected final RestTemplateCustomizer restTemplateCustomizer;
    protected final SupplierLogger log;
    protected final AuthService auth;

    private final UriTemplate uriTemplate;
    private final CyodaApiRequestStatsMonitor requestStatsMonitor;


    @Inject
    public ReportConfigDetailsApiHttp(CyodaConnectorId connectorId, CyodaConfig config, TypeManager typeManager,
                                      RestTemplateCustomizer restTemplateCustomizer,
                                      AuthService authService,
                                      CyodaApiRequestStatsMonitor requestStatsMonitor) {
        this.config = requireNonNull(config, "config is null");
        this.restTemplateCustomizer = restTemplateCustomizer;
        this.log = LOG;
        this.auth = authService;
        this.requestStatsMonitor = requestStatsMonitor;
        this.typeManager = typeManager;
        uriTemplate = setupUriTemplate();
    }

    private static String asString(Object me, HttpClientErrorException e) {
        return toStringHelper(me)
                .add("statusCode", e.getStatusCode())
                .add("statusMessage", e.getStatusText())
                .add("headers", e.getResponseHeaders())
                .omitNullValues()
                .toString();
    }


    @Override
    public ReportDefinitionHandle getReportDefSingleHandle(ReportConfigKey reportConfigKey) {
        String reportConfigId = reportConfigKey.configId();
        Map<String, Object> expansion = Collections.singletonMap(REPORT_ID_COLUMN, reportConfigId);
        URI templatedUri = uriTemplate.expand(expansion);

        Date callDate = new Date();
        Traverson traverson = new Traverson(templatedUri, MediaTypes.HAL_JSON);
        traverson.setRestOperations(getTechRestTemplate());
        String jsonResult;
        try {
            jsonResult = Optional.ofNullable(traverson
                            .follow()
                            .toEntity(String.class)).map(ResponseEntity::getBody)
                    .orElseThrow(() -> new IllegalArgumentException("No body found at " + templatedUri));
        } catch (HttpClientErrorException e) {
            throw requestFailedException(this, e, templatedUri);
        } finally {
            registerApiCall(reportConfigKey.queryId(), callDate, templatedUri.toString(), expansion);
        }
        return ReportConfigParser.parseReportDefinitionHandle(jsonResult, reportConfigId, typeManager);
    }


    private UriTemplate setupUriTemplate() {

        URI uri;
        try {
            uri = config.getServerUrl().toURI().resolve(REPORT_DETAILS_ENDPOINT);
        } catch (URISyntaxException e) {
            throw new TrinoException(StandardErrorCode.GENERIC_INTERNAL_ERROR, e);
        }
        final ImmutableList.Builder<TemplateVariable> builder = ImmutableList.builder();
        builder.add(TemplateVariable.pathVariable("id"));
        builder.add();

        TemplateVariables vars = new TemplateVariables(builder.build());
        return UriTemplate.of(uri.toASCIIString()).with(vars);
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

    protected void registerApiCall(String queryId, Date callTime, String requestUrl, Map<String, Object> params){
        requestStatsMonitor.registerApiCall(queryId, callTime, requestUrl, params, getName());
    }

    public String getName(){
        return this.getClass().getSimpleName().replaceAll("ApiHandler", "");
    }

    protected RestTemplate getTechRestTemplate() {
        return restTemplateCustomizer.getRestTemplate(auth.getTechnicalAuth());
    }


}
