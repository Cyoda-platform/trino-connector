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

import com.cyoda.api.view.GridConfigFieldsView;
import com.cyoda.presto.client.ApiRequestHandler;
import com.cyoda.presto.client.CyodaApiRequestHandlerProvider;
import com.cyoda.presto.client.RestTemplateCustomizer;
import com.cyoda.presto.client.logic.Any;
import com.cyoda.presto.client.logic.PredicateBuilder;
import com.cyoda.presto.client.logic.PredicateNode;
import com.cyoda.presto.client.reporting.ConfiguredReportsApiHandler;
import com.cyoda.presto.client.reporting.CyodaStaticReportTable;
import com.cyoda.presto.handles.CyodaColumnHandle;
import com.cyoda.presto.handles.CyodaTableHandle;
import com.facebook.presto.common.Page;
import com.facebook.presto.common.predicate.TupleDomain;
import com.facebook.presto.common.type.TimestampType;
import com.facebook.presto.common.type.TypeManager;
import com.facebook.presto.common.type.TypeSignature;
import com.facebook.presto.common.type.VarcharType;
import com.facebook.presto.spi.ColumnHandle;
import com.facebook.presto.spi.RecordSet;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.client.Traverson;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.*;

@SuppressWarnings("UnstableApiUsage")
public class TestCyodaRecordSet {
    CyodaConnectorId connectorId = new CyodaConnectorId("conn-id-1");
    String requestHandlerKey = CyodaStaticReportTable.REPORTS.name();
    private LocalHttpServer ourHttpServer;

    @Test
    public void testPage1ConfiguredReports() throws IOException, URISyntaxException {
        CyodaConfig testCyodaConfig = createCyodaConfig();

        CyodaApiRequestHandlerProvider handlerProvider = setupHandlerProvider(testCyodaConfig);
        CyodaClient client = new CyodaClient(connectorId, testCyodaConfig, handlerProvider);

        ApiRequestHandler<?> apiHandler = handlerProvider.getHandler(requestHandlerKey);
        List<CyodaTable> tables = apiHandler.getTables();
        assertTrue(apiHandler instanceof ConfiguredReportsApiHandler);
        assertEquals(tables.size(), 1); // There is only one table for that.

        setupReponseMapper();

        URI dataUri = ourHttpServer.getBaseUri().resolve(ConfiguredReportsApiHandler.REPORT_DEFS_ENDPOINT);

        TupleDomain<ColumnHandle> constraint = TupleDomain.all();
        CyodaTableHandle tableHandle = new CyodaTableHandle(connectorId.toString(), "schema", "table", Optional.empty(), requestHandlerKey);
//        RecordSet recordSet = new CyodaRecordSet<PagedModel<GridConfigFieldsView>,GridConfigFieldsView>(
//                client,
//                new CyodaSplit(tableHandle, dataUri, constraint),
//                tables.get(0).getColumns()
//        );
//        RecordCursor cursor = recordSet.cursor();
//        assertNotNull(cursor);
//        int cnt = 0;
//        while (cursor.advanceNextPosition()) {
//            cnt++;
//            Slice id = cursor.getSlice(0);
//            assertNotNull(id.toStringUtf8());
//        }
//        assertEquals(cnt, DEFAULT_PAGE_SIZE);


    }

    private CyodaApiRequestHandlerProvider setupHandlerProvider(CyodaConfig mockCyodaConfig) throws URISyntaxException {
        TypeManager mockTypeManager = mock(TypeManager.class);
        TypeSignature varcharTypeSig = new TypeSignature(VarcharType.VARCHAR.getTypeSignature().getBase());
        TypeSignature localDateTimeSig = new TypeSignature(TimestampType.TIMESTAMP.getTypeSignature().getBase());
        when(mockTypeManager.getType(varcharTypeSig)).thenReturn(VarcharType.VARCHAR);
        when(mockTypeManager.getType(localDateTimeSig)).thenReturn(TimestampType.TIMESTAMP);

        HttpMessageConverter<?> converter = Traverson.getDefaultMessageConverters(MediaTypes.HAL_JSON).stream()
                .filter(it -> MappingJackson2HttpMessageConverter.class.isAssignableFrom(it.getClass()))
                .findAny().orElseThrow(() -> new RuntimeException("not found"));

        RestTemplateCustomizer restTemplateCustomizer = new RestTemplateCustomizer(mockCyodaConfig);
        @SuppressWarnings("rawtypes")
        Set<ApiRequestHandler> handlers = Collections.singleton(
                new ConfiguredReportsApiHandler(connectorId, mockCyodaConfig, mockTypeManager, restTemplateCustomizer)
        );

        CyodaApiRequestHandlerProvider handlerProvider = new CyodaApiRequestHandlerProvider(handlers);
        return handlerProvider;
    }

    private void setupReponseMapper() {
        ResponseMapper mockMapper = new ResponseMapper() {
            final String page1ResourcePath = "/reporting-responses/get-report-defs-page1.json";
            final String page2ResourcePath = "/reporting-responses/get-report-defs-page2.json";
            final String page3ResourcePath = "/reporting-responses/get-report-defs-page3.json";
            final String[] resourcePaths = {page1ResourcePath,page2ResourcePath,page3ResourcePath};

            final List<URL> responses = Arrays.stream(resourcePaths).map(
                    it -> Resources.getResource(TestCyodaRecordSet.class, it)
            ).collect(Collectors.toList());
            int pos = 0;
            @Override
            public String resolveResponse() throws IOException {
                return Resources.toString(responses.get(pos++), StandardCharsets.UTF_8);
            }
        };

        ImmutableMap<String, ResponseMapper> pathInfoMapper = ImmutableMap.of(
                ConfiguredReportsApiHandler.REPORT_DEFS_ENDPOINT, mockMapper
        );

        ResponseMapperProvider provider = new ResponseMapperProvider().withResponseMapperProvider(pathInfoMapper);
        ourHttpServer.setResponseMapperProvider(provider);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testPagedIteratorWithConfiguredReports() throws IOException, URISyntaxException {
        CyodaConfig testCyodaConfig = createCyodaConfig();

        CyodaApiRequestHandlerProvider handlerProvider = setupHandlerProvider(testCyodaConfig);
        CyodaClient client = new CyodaClient(connectorId, testCyodaConfig, handlerProvider);

        ApiRequestHandler<GridConfigFieldsView> apiHandler =
                (ApiRequestHandler<GridConfigFieldsView>) handlerProvider.getHandler(requestHandlerKey);
        List<CyodaTable> tables = apiHandler.getTables();
        assertTrue(apiHandler instanceof ConfiguredReportsApiHandler);
        assertEquals(tables.size(), 1); // There is only one table for that.

        setupReponseMapper();

        TupleDomain<CyodaColumnHandle> constraint = TupleDomain.all();
        CyodaTableHandle tableHandle = new CyodaTableHandle(connectorId.toString(), "schema", "table", Optional.empty(), requestHandlerKey);
        PredicateNode<Any> predicates = PredicateBuilder.setupConstraintPredicates(TupleDomain.all());
        CyodaFilteringPageSource<GridConfigFieldsView> pageSource =
                new CyodaFilteringPageSource<>(apiHandler, tableHandle, tables.get(0).getColumns(), client, predicates);

        assertNotNull(pageSource);
        int total = 0;
        while (!pageSource.isFinished()) {
            Page page = pageSource.getNextPage();
            boolean isFinished = pageSource.isFinished();
            assertNotNull(page);
            assertEquals(page.getChannelCount(), tables.get(0).getColumns().size());
            total += page.getPositionCount();
        }
        // The source shall only read up to the page size, even if there are more elements.
        // get-report-defs-page1.json and page2 have 11 configured reports,
        // pag3 has 5 reports.
        // The page size is 10, however. So we expect a count of 10+10+5
        assertEquals(total,25);
    }

    private CyodaConfig createCyodaConfig() throws MalformedURLException {
        return new CyodaConfig()
                .setServerUrl(ourHttpServer.resolve("").toURL())
                .setRequestPageSize(10);
    }

    @Test
    public void testThatColumnTypesAreCorrect() throws MalformedURLException, URISyntaxException {
        CyodaConfig testCyodaConfig = createCyodaConfig();

        CyodaApiRequestHandlerProvider handlerProvider = setupHandlerProvider(testCyodaConfig);
        CyodaClient client = new CyodaClient(connectorId, testCyodaConfig, handlerProvider);

        URI dataUri = ourHttpServer.getBaseUri().resolve(ConfiguredReportsApiHandler.REPORT_DEFS_ENDPOINT);

        RecordSet recordSet;
        CyodaTableHandle tableHandle = new CyodaTableHandle(connectorId.toString(), "schema", "table", Optional.empty(), requestHandlerKey);

//        recordSet = new CyodaRecordSet<>(client, new CyodaSplit(tableHandle, dataUri, TupleDomain.all()), ImmutableList.of(
//                new CyodaColumnHandle("test", "value", BIGINT, DataType.BIG_INTEGER, 1, requestHandlerKey),
//                new CyodaColumnHandle("test", "text", createUnboundedVarcharType(), DataType.STRING, 0, requestHandlerKey)));
//        assertEquals(recordSet.getColumnTypes(), ImmutableList.of(BIGINT, createUnboundedVarcharType()));
//
//        recordSet = new CyodaRecordSet<>(client, new CyodaSplit(tableHandle, dataUri, TupleDomain.all()), ImmutableList.of(
//                new CyodaColumnHandle("test", "value", BIGINT, DataType.BIG_INTEGER, 1, requestHandlerKey),
//                new CyodaColumnHandle("test", "value", BIGINT, DataType.BIG_INTEGER, 1, requestHandlerKey),
//                new CyodaColumnHandle("test", "text", createUnboundedVarcharType(), DataType.STRING, 0, requestHandlerKey)));
//        assertEquals(recordSet.getColumnTypes(), ImmutableList.of(BIGINT, BIGINT, createUnboundedVarcharType()));
//
//        recordSet = new CyodaRecordSet<>(client, new CyodaSplit(tableHandle, dataUri, TupleDomain.none()), ImmutableList.of());
//        assertEquals(recordSet.getColumnTypes(), ImmutableList.of());
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
