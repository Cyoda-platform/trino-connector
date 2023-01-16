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

package com.cyoda.presto.client.reporting.data;

import com.cyoda.presto.CyodaConfig;
import com.cyoda.presto.CyodaConnectorId;
import com.cyoda.presto.CyodaSplit;
import com.cyoda.presto.auth.AuthService;
import com.cyoda.presto.client.RestTemplateCustomizer;
import com.cyoda.presto.client.reporting.BaseReportsApiHandler;
import com.cyoda.presto.client.reporting.stats.CyodaApiRequestStatsMonitor;
import com.cyoda.presto.logging.SupplierLogger;
import com.cyoda.service.api.beans.ReportRow;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.trino.spi.StandardErrorCode;
import io.trino.spi.TrinoException;
import io.trino.spi.type.TypeManager;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.TemplateVariable;
import org.springframework.hateoas.TemplateVariables;
import org.springframework.hateoas.UriTemplate;
import org.springframework.hateoas.client.Traverson;
import org.springframework.hateoas.server.core.TypeReferences;
import org.springframework.web.client.HttpClientErrorException;

import javax.inject.Inject;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static com.cyoda.presto.client.ExceptionsUtil.requestFailedException;
import static com.cyoda.presto.client.reporting.metaproviders.StaticReportFields.ROW_GROUP_JSON_BASE64_VARIABLE;
import static com.cyoda.presto.client.reporting.metaproviders.StaticReportFields.ROW_REPORT_ID_COLUMN;

public class ReportRowsApiHandler extends BaseReportsApiHandler {

    protected static final SupplierLogger LOG = SupplierLogger.get(ReportRowsApiHandler.class);

    static final String REPORT_ROWS_TEMPLATE = "/{" + ROW_REPORT_ID_COLUMN + "" +
            "}/group_rows/{" +
            ROW_GROUP_JSON_BASE64_VARIABLE + "}";

    @Inject
    public ReportRowsApiHandler(CyodaConnectorId connectorId, CyodaConfig config, TypeManager typeManager,
                                RestTemplateCustomizer restTemplateCustomizer,
                                AuthService authService,
                                CyodaApiRequestStatsMonitor requestStatsMonitor) {
        super(connectorId, config, typeManager, restTemplateCustomizer, LOG, authService, requestStatsMonitor);
    }


    private UriTemplate setupUriTemplate() {

        URI uri;
        try {
            uri = config.getServerUrl().toURI().resolve(REPORT_ENDPOINT);
        } catch (URISyntaxException e) {
            throw new TrinoException(StandardErrorCode.GENERIC_INTERNAL_ERROR, e);
        }
        final ImmutableList.Builder<TemplateVariable> builder = ImmutableList.builder();
        builder.add(
                TemplateVariable.requestParameter(PAGE_REQUEST_PARAMETER),
                TemplateVariable.requestParameterContinued(SIZE_REQUEST_PARAMETER)
        );

        TemplateVariables vars = new TemplateVariables(builder.build());
        return UriTemplate.of(uri.toASCIIString() + REPORT_ROWS_TEMPLATE)
                .with(vars);
    }




    public Iterable<RowHandle> getIterable(CyodaSplit split) {

        //        int size = (pageSize == 0) ? DEFAULT_PAGE_SIZE : pageSize;
        UriTemplate uriTemplate = setupUriTemplate();

        long startTime = System.currentTimeMillis();
        RowNumHandle rowNumHandle = RowNumHandle.getSimpleHandle(split.getPage(), split.getSize());
        ImmutableMap.Builder<String, Object> expansionBuilder = ImmutableMap.<String, Object>builder()
                .put(PAGE_REQUEST_PARAMETER, rowNumHandle.page)
                .put(SIZE_REQUEST_PARAMETER, rowNumHandle.size);


        expansionBuilder.put(ROW_REPORT_ID_COLUMN, split.getReportId());
        expansionBuilder.put(ROW_GROUP_JSON_BASE64_VARIABLE, split.getGroupJsonBase64());

        ImmutableMap<String, Object> expansion = expansionBuilder.build();
        URI templatedUri = uriTemplate.expand(expansion);

        Date apiCallTime = new Date();
        Traverson traverson = new Traverson(templatedUri, MediaTypes.HAL_JSON);
        traverson.setRestOperations(restTemplateCustomizer.getRestTemplateWithTechAuth());

        TypeReferences.PagedModelType<ReportRow> typeReference =
                new TypeReferences.PagedModelType<ReportRow>() {
                };

        try {
            final PagedModel<ReportRow> fieldsViews = traverson
                    .follow()
                    .toObject(typeReference);
            if (fieldsViews == null) return Collections.emptyList();
            registerApiCall(split.getQueryId(), apiCallTime, templatedUri.toString(), expansion);
            AtomicLong rowNum = new AtomicLong(rowNumHandle.offset);
            return fieldsViews.getContent().stream()
                    .map(reportRow ->
                            new RowHandle(split.getReportId(),
                                    split.getGroupingVersion(),
                                    split.getGroupJsonBase64(),
                                    reportRow, rowNum.incrementAndGet()))
                    .filter(reportRow -> rowNumHandle.isInRowWindow(reportRow.rowNum()))
                    .limit(rowNumHandle.size) // This to ringfence buggy API that sends one than the page size.
                    .collect(Collectors.toList());
        } catch (HttpClientErrorException e) {
            throw requestFailedException(this, "retrieveCollection", e, templatedUri);
        }
    }

}

