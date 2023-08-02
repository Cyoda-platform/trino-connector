///*
// * Copyright (C) 2022 Cyoda Ltd.
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *      http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// *
// */
//package com.cyoda.presto;
//
//import com.cyoda.api.view.GridConfigFieldsView;
//import com.cyoda.presto.auth.AuthContext;
//import com.cyoda.presto.client.RestTemplateCustomizer;
//import com.cyoda.presto.client.logic.Any;
//import com.cyoda.presto.client.logic.ColumnPredicateBuilder;
//import com.cyoda.presto.client.logic.CompoundPredicateNode;
//import com.cyoda.presto.client.reporting.meta.ReportConfigDetailsApiHandler;
//import com.cyoda.presto.client.reporting.meta.ReportHistoryApiHandler;
//import com.cyoda.presto.client.reporting.meta.ReportStatisticsApiHandler;
//import com.cyoda.presto.client.reporting.metaproviders.StaticTableMetadataProvider;
//import com.cyoda.presto.client.reporting.metaproviders.StaticTableMetadata;
//import com.cyoda.presto.client.reporting.meta.ConfiguredReportsApiHandler;
//import com.cyoda.presto.client.types.CompoundDataType;
//import com.cyoda.presto.client.types.DataType;
//import com.cyoda.presto.handles.CyodaColumnHandle;
//import com.cyoda.presto.handles.CyodaTableHandle;
//import io.trino.spi.Page;
//import io.trino.spi.predicate.TupleDomain;
//import io.trino.spi.type.TimestampType;
//import io.trino.spi.type.TypeManager;
//import io.trino.spi.type.TypeSignature;
//import io.trino.spi.type.VarcharType;
//import com.google.common.collect.ImmutableMap;
//import com.google.common.io.Resources;
//import org.testng.annotations.AfterClass;
//import org.testng.annotations.BeforeClass;
//import org.testng.annotations.Test;
//
//import java.io.IOException;
//import java.net.MalformedURLException;
//import java.net.URL;
//import java.nio.charset.StandardCharsets;
//import java.util.Arrays;
//import java.util.Collections;
//import java.util.List;
//import java.util.Optional;
//import java.util.stream.Collectors;
//
//import static org.mockito.Mockito.mock;
//import static org.mockito.Mockito.when;
//import static org.testng.Assert.*;
//
//@SuppressWarnings("UnstableApiUsage")
//public class TestCyodaRecordSet {
//    CyodaConnectorId connectorId = new CyodaConnectorId("conn-id-1");
//    String requestHandlerKey = StaticTableMetadata.REPORTS.name();
//    private LocalHttpServer ourHttpServer;
//
//    private CyodaApiRequestHandlerProvider setupHandlerProvider(CyodaConfig mockCyodaConfig) {
//        TypeManager mockTypeManager = mock(TypeManager.class);
//        TypeSignature varcharTypeSig = new TypeSignature(VarcharType.VARCHAR.getTypeSignature().getBase());
//        TypeSignature localDateTimeSig = new TypeSignature(TimestampType.TIMESTAMP_MILLIS.getTypeSignature().getBase());
//        when(mockTypeManager.getType(varcharTypeSig)).thenReturn(VarcharType.VARCHAR);
//        when(mockTypeManager.getType(localDateTimeSig)).thenReturn(TimestampType.TIMESTAMP_MILLIS);
//
//        RestTemplateCustomizer restTemplateCustomizer = new RestTemplateCustomizer(mockCyodaConfig);
//        StaticTableMetadataProvider staticReportMetadataProvider = mock(StaticTableMetadataProvider.class);
//        StaticTableMetadataProvider.Reports reports = mock(StaticTableMetadataProvider.Reports.class);
//        when(reports.getTypeColumn()).thenReturn(new CyodaColumnHandle("type", VarcharType.VARCHAR, new CompoundDataType("", DataType.STRING),2, true));
//        when(staticReportMetadataProvider.getReports()).thenReturn(reports);
//
//        ConfiguredReportsApiHandler reportsApiHandler = new ConfiguredReportsApiHandler(connectorId, mockCyodaConfig, mockTypeManager, restTemplateCustomizer, staticReportMetadataProvider);
//
//        return new CyodaApiRequestHandlerProvider(
//                reportsApiHandler,
//                mock(ReportConfigDetailsApiHandler.class),
//                mock(ReportStatisticsApiHandler.class),
//                mock(ReportHistoryApiHandler.class),
//                mock(ReportGroupsApiHandler.class),
//                mock(ReportRowsApiHandler.class));
//    }
//
//    private void setupReponseMapper() {
//        ResponseMapper mockMapper = new ResponseMapper() {
//            final String page1ResourcePath = "/reporting-responses/get-report-defs-page1.json";
//            final String page2ResourcePath = "/reporting-responses/get-report-defs-page2.json";
//            final String page3ResourcePath = "/reporting-responses/get-report-defs-page3.json";
//            final String[] resourcePaths = {page1ResourcePath,page2ResourcePath,page3ResourcePath};
//
//            final List<URL> responses = Arrays.stream(resourcePaths).map(
//                    it -> Resources.getResource(TestCyodaRecordSet.class, it)
//            ).collect(Collectors.toList());
//            int pos = 0;
//            @Override
//            public Optional<String> resolveResponse() throws IOException {
//                return responses.size()>pos ? Optional.of(Resources.toString(responses.get(pos++), StandardCharsets.UTF_8)) :
//                        Optional.empty();
//            }
//        };
//
//        ImmutableMap<String, ResponseMapper> pathInfoMapper = ImmutableMap.of(
//                ConfiguredReportsApiHandler.REPORT_DEFS_ENDPOINT, mockMapper
//        );
//
//        ResponseMapperProvider provider = new ResponseMapperProvider().withResponseMapperProvider(pathInfoMapper);
//        ourHttpServer.setResponseMapperProvider(provider);
//    }
//
//    @SuppressWarnings("unchecked")
//    @Test
//    public void testPagedIteratorWithConfiguredReports() throws IOException {
//        CyodaConfig testCyodaConfig = createCyodaConfig();
//
//        CyodaApiRequestHandlerProvider handlerProvider = setupHandlerProvider(testCyodaConfig);
//
//        ApiRequestHandler<Any, GridConfigFieldsView> apiHandler =
//                (ApiRequestHandler<Any, GridConfigFieldsView>) handlerProvider.getHandler(requestHandlerKey);
//        AuthContext authContext = mock(AuthContext.class);
//
//
//        setupReponseMapper();
//
//        CyodaTableHandle tableHandle = new CyodaTableHandle(connectorId.toString(), "schema", "table", Collections.emptyList(), requestHandlerKey, null, null, null);
//        CompoundPredicateNode predicates = ColumnPredicateBuilder.setupConstraintPredicates(TupleDomain.all());
//        CyodaFilteringPageSource<Any, GridConfigFieldsView> pageSource =
//                new CyodaFilteringPageSource<>(authContext, apiHandler, tableHandle, tableHandle.getProjectedColumns(), null, predicates);
//
//        assertNotNull(pageSource);
//        int total = 0;
//        while (!pageSource.isFinished()) {
//            Page page = pageSource.getNextPage();
//            assertNotNull(page);
//            assertEquals(page.getChannelCount(), tableHandle.getProjectedColumns().size());
//            total += page.getPositionCount();
//        }
//        // The source shall only read up to the page size, even if there are more elements.
//        // get-report-defs-page1.json and page2 have 11 configured reports,
//        // pag3 has 5 reports.
//        // The page size is 10, however. So we expect a count of 10+10+5
//        assertEquals(total,25);
//    }
//
//    private CyodaConfig createCyodaConfig() throws MalformedURLException {
//        return new CyodaConfig()
//                .setServerUrl(ourHttpServer.resolve("").toURL())
//                .setRequestPageSize(10);
//    }
//
//    @BeforeClass
//    public void setUpClass() {
//        ourHttpServer = new LocalHttpServer();
//    }
//
//    @AfterClass(alwaysRun = true)
//    public void tearDown() {
//        if (ourHttpServer != null) {
//            ourHttpServer.stop();
//        }
//    }
//}
