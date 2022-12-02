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

package com.cyoda.presto.client;

import com.cyoda.presto.client.reporting.data.ReportRowsApiHandler;
import com.cyoda.presto.client.reporting.groups.ReportGroupsApiHandler;
import com.cyoda.presto.client.reporting.meta.ConfiguredReportsApiHandler;
import com.cyoda.presto.client.reporting.meta.ReportConfigDetailsApiHandler;
import com.cyoda.presto.client.reporting.meta.ReportHistoryApiHandler;
import com.cyoda.presto.client.reporting.meta.ReportStatisticsApiHandler;
import com.cyoda.presto.client.reporting.metaproviders.StaticReportTable;
import com.google.common.collect.ImmutableMap;

import javax.inject.Inject;
import java.util.Map;

import static java.util.Objects.requireNonNull;

public class CyodaApiRequestHandlerProvider {

    private final Map<StaticReportTable, ApiRequestHandler<?>> handlers;


    @SuppressWarnings({"squid:S3740"})
    @Inject
    public CyodaApiRequestHandlerProvider(ConfiguredReportsApiHandler configuredReportsApiHandler,
                                          ReportConfigDetailsApiHandler reportConfigDetailsApiHandler,
                                          ReportStatisticsApiHandler reportStatisticsApiHandler,
                                          ReportHistoryApiHandler reportHistoryApiHandler,
                                          ReportGroupsApiHandler reportGroupsApiHandler,
                                          ReportRowsApiHandler reportRowsApiHandler) {
        handlers = ImmutableMap.<StaticReportTable, ApiRequestHandler<?>>builder()
                .put(StaticReportTable.REPORTS, configuredReportsApiHandler)
                .put(StaticReportTable.REPORT_DETAILS, reportConfigDetailsApiHandler)
                .put(StaticReportTable.REPORT_STATS, reportStatisticsApiHandler)
                .put(StaticReportTable.REPORT_HISTORIES, reportHistoryApiHandler)
                .put(StaticReportTable.REPORT_GROUPS, reportGroupsApiHandler)
                .put(StaticReportTable.REPORT_ROWS, reportRowsApiHandler)
                .build();

    }

    @SuppressWarnings({"java:S1452"})
    public ApiRequestHandler<?> getHandler(String type) {
        requireNonNull(type, "type is null");
        return handlers.get(StaticReportTable.valueOf(type));
    }

}
