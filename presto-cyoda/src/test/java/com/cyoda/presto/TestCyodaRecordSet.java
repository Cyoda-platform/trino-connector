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
package com.cyoda.presto;

import com.cyoda.presto.handles.CyodaColumnHandle;
import com.cyoda.presto.client.neededatcyoda.GridConfigFieldsView;
import com.cyoda.presto.client.reporting.ConfiguredReportsApiHandler;
import com.cyoda.presto.client.CyodaApiRequestHandler;
import com.cyoda.presto.client.CyodaApiRequestHandlerProvider;
import com.cyoda.presto.client.reporting.CyodaStaticReportTable;
import com.facebook.presto.spi.RecordCursor;
import com.facebook.presto.spi.RecordSet;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import io.airlift.slice.Slice;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.facebook.presto.common.type.BigintType.BIGINT;
import static com.facebook.presto.common.type.VarcharType.createUnboundedVarcharType;
import static org.testng.Assert.*;

@SuppressWarnings("UnstableApiUsage")
public class TestCyodaRecordSet {
    CyodaConnectorId connectorId = new CyodaConnectorId("conn-id-1");
    String requestHandlerKey = CyodaStaticReportTable.REPORTS.name();
    private LocalHttpServer ourHttpServer;

    @Test
    public void testPage1ConfiguredReports() throws IOException, URISyntaxException {
        CyodaConfig mockCyodaConfig = createCyodaConfig();

        @SuppressWarnings("rawtypes")
        Set<CyodaApiRequestHandler> handlers = Collections.singleton(new ConfiguredReportsApiHandler(connectorId, mockCyodaConfig));

        CyodaApiRequestHandlerProvider handlerProvider = new CyodaApiRequestHandlerProvider(handlers);
        CyodaClient client = new CyodaClient(connectorId, mockCyodaConfig, handlerProvider);

        CyodaApiRequestHandler<?> apiHandler = handlerProvider.getHandler(requestHandlerKey);
        List<CyodaTable> tables = apiHandler.getTables();
        assertTrue(apiHandler instanceof ConfiguredReportsApiHandler);
        assertEquals(tables.size(), 1); // There is only one table for that.


        URI dataUri = ourHttpServer.getBaseUri().resolve(ConfiguredReportsApiHandler.REPORT_DEFS_ENDPOINT);

        ResponseMapper mockMapper = new ResponseMapper() {
            final String page1ResourcePath = "/reporting-responses/get-report-defs-page1.json";
            final URL dataUrl = Resources.getResource(TestCyodaRecordSet.class, page1ResourcePath);

            @Override
            public String resolveResponse() throws IOException {
                return Resources.toString(dataUrl, StandardCharsets.UTF_8);
            }
        };

        ImmutableMap<String, ResponseMapper> pathInfoMapper = ImmutableMap.of(
                ConfiguredReportsApiHandler.REPORT_DEFS_ENDPOINT, mockMapper
        );

        ResponseMapperProvider provider = new ResponseMapperProvider().withResponseMapperProvider(pathInfoMapper);
        ourHttpServer.setResponseMapperProvider(provider);

        RecordSet recordSet = new CyodaRecordSet<GridConfigFieldsView>(
                client,
                new CyodaSplit(connectorId.toString(), "schema", "table", dataUri, requestHandlerKey),
                tables.get(0).getColumns()
        );
        RecordCursor cursor = recordSet.cursor();
        assertNotNull(cursor);
        boolean okay = cursor.advanceNextPosition();
        assertTrue(okay);
        Slice id = cursor.getSlice(0);
        assertNotNull(id.toStringUtf8());


    }

    private CyodaConfig createCyodaConfig() throws MalformedURLException {
        return new CyodaConfig()
                .setServerUrl(ourHttpServer.resolve("").toURL());
    }

    @Test
    public void testThatColumnTypesAreCorrect() throws MalformedURLException, URISyntaxException {
        CyodaConfig mockCyodaConfig = createCyodaConfig();

        @SuppressWarnings("rawtypes")
        Set<CyodaApiRequestHandler> handlers = Collections.singleton(new ConfiguredReportsApiHandler(connectorId, mockCyodaConfig));

        CyodaApiRequestHandlerProvider handlerProvider = new CyodaApiRequestHandlerProvider(handlers);
        CyodaClient client = new CyodaClient(connectorId, mockCyodaConfig, handlerProvider);

        URI dataUri = ourHttpServer.getBaseUri().resolve(ConfiguredReportsApiHandler.REPORT_DEFS_ENDPOINT);

        RecordSet recordSet;
        recordSet = new CyodaRecordSet<>(client, new CyodaSplit("test", "schema", "table", dataUri, requestHandlerKey), ImmutableList.of(
                new CyodaColumnHandle("test", "value", BIGINT, 1, requestHandlerKey),
                new CyodaColumnHandle("test", "text", createUnboundedVarcharType(), 0, requestHandlerKey)));
        assertEquals(recordSet.getColumnTypes(), ImmutableList.of(BIGINT, createUnboundedVarcharType()));

        recordSet = new CyodaRecordSet<>(client, new CyodaSplit("test", "schema", "table", dataUri, requestHandlerKey), ImmutableList.of(
                new CyodaColumnHandle("test", "value", BIGINT, 1, requestHandlerKey),
                new CyodaColumnHandle("test", "value", BIGINT, 1, requestHandlerKey),
                new CyodaColumnHandle("test", "text", createUnboundedVarcharType(), 0, requestHandlerKey)));
        assertEquals(recordSet.getColumnTypes(), ImmutableList.of(BIGINT, BIGINT, createUnboundedVarcharType()));

        recordSet = new CyodaRecordSet<>(client, new CyodaSplit("test", "schema", "table", dataUri, requestHandlerKey), ImmutableList.of());
        assertEquals(recordSet.getColumnTypes(), ImmutableList.of());
    }

    @BeforeClass
    public void setUpClass() {
        ourHttpServer = new LocalHttpServer();
    }

    @AfterClass(alwaysRun = true)
    public void tearDown() {
        if (ourHttpServer != null) {
            ourHttpServer.stop();
        }
    }
}
