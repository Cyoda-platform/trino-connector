/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cyoda.presto;

import com.cyoda.presto.handles.CyodaColumnHandle;
import com.cyoda.presto.http.QueryRunner;
import com.facebook.presto.example.ExampleColumnHandle;
import com.facebook.presto.example.ExampleRecordSet;
import com.facebook.presto.example.ExampleSplit;
import com.facebook.presto.spi.RecordCursor;
import com.facebook.presto.spi.RecordSet;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.facebook.presto.common.type.BigintType.BIGINT;
import static com.facebook.presto.common.type.VarcharType.createUnboundedVarcharType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

@SuppressWarnings("UnstableApiUsage")
public class TestCyodaRecordSet
{
    private OurHttpServer ourHttpServer;
    private URI dataUri;

    @Test
    public void testGetColumnTypes() throws IOException {
        String page1Path = "/platform-api/reporting/definitions?page=0";
        dataUri = ourHttpServer.resolve(page1Path);

        ResponseMapper mapper = mock(ResponseMapper.class);
        ImmutableMap<String, ResponseMapper> pathInfoMapper = ImmutableMap.of(
                page1Path,mapper
        );
        String page1ResourcePath = "/reporting-responses/get-report-defs-page1.json";
        URL dataUrl = Resources.getResource(TestCyodaRecordSet.class, page1ResourcePath);
        String answer = Resources.toString(dataUrl, StandardCharsets.UTF_8);
        when(mapper.resolveResponse())
                .thenReturn(answer);

        ResponseMapperProvider provider = new ResponseMapperProvider().withResponseMapperProvider(pathInfoMapper);
        ourHttpServer.setResponseMapperProvider(provider);

        CyodaConfig config = mock(CyodaConfig.class);
        QueryRunner queryRunner = new QueryRunner(config);
        CyodaClient client = new CyodaClient(new CyodaConnectorId("conn-id-1"),config,queryRunner);

        RecordSet recordSet = new CyodaRecordSet(client,new CyodaSplit("test", "schema", "table", dataUri), ImmutableList.of(
                new CyodaColumnHandle("test", "text", createUnboundedVarcharType(), 0),
                new CyodaColumnHandle("test", "value", BIGINT, 1)));
        assertEquals(recordSet.getColumnTypes(), ImmutableList.of(createUnboundedVarcharType(), BIGINT));

        recordSet = new CyodaRecordSet(client,new CyodaSplit("test", "schema", "table", dataUri), ImmutableList.of(
                new CyodaColumnHandle("test", "value", BIGINT, 1),
                new CyodaColumnHandle("test", "text", createUnboundedVarcharType(), 0)));
        assertEquals(recordSet.getColumnTypes(), ImmutableList.of(BIGINT, createUnboundedVarcharType()));

        recordSet = new CyodaRecordSet(client,new CyodaSplit("test", "schema", "table", dataUri), ImmutableList.of(
                new CyodaColumnHandle("test", "value", BIGINT, 1),
                new CyodaColumnHandle("test", "value", BIGINT, 1),
                new CyodaColumnHandle("test", "text", createUnboundedVarcharType(), 0)));
        assertEquals(recordSet.getColumnTypes(), ImmutableList.of(BIGINT, BIGINT, createUnboundedVarcharType()));

        recordSet = new CyodaRecordSet(client,new CyodaSplit("test", "schema", "table", dataUri), ImmutableList.of());
        assertEquals(recordSet.getColumnTypes(), ImmutableList.of());
    }

    @Test
    public void testCursorSimple() {
        RecordSet recordSet = new ExampleRecordSet(new ExampleSplit("test", "schema", "table", dataUri), ImmutableList.of(
                new ExampleColumnHandle("test", "text", createUnboundedVarcharType(), 0),
                new ExampleColumnHandle("test", "value", BIGINT, 1)));
        RecordCursor cursor = recordSet.cursor();

        assertEquals(cursor.getType(0), createUnboundedVarcharType());
        assertEquals(cursor.getType(1), BIGINT);

        Map<String, Long> data = new LinkedHashMap<>();
        while (cursor.advanceNextPosition()) {
            data.put(cursor.getSlice(0).toStringUtf8(), cursor.getLong(1));
            assertFalse(cursor.isNull(0));
            assertFalse(cursor.isNull(1));
        }
        assertEquals(data, ImmutableMap.<String, Long>builder()
                .put("ten", 10L)
                .put("eleven", 11L)
                .put("twelve", 12L)
                .build());
    }

    @Test
    public void testCursorMixedOrder() {
        RecordSet recordSet = new ExampleRecordSet(new ExampleSplit("test", "schema", "table", dataUri), ImmutableList.of(
                new ExampleColumnHandle("test", "value", BIGINT, 1),
                new ExampleColumnHandle("test", "value", BIGINT, 1),
                new ExampleColumnHandle("test", "text", createUnboundedVarcharType(), 0)));
        RecordCursor cursor = recordSet.cursor();

        Map<String, Long> data = new LinkedHashMap<>();
        while (cursor.advanceNextPosition()) {
            assertEquals(cursor.getLong(0), cursor.getLong(1));
            data.put(cursor.getSlice(2).toStringUtf8(), cursor.getLong(0));
        }
        assertEquals(data, ImmutableMap.<String, Long>builder()
                .put("ten", 10L)
                .put("eleven", 11L)
                .put("twelve", 12L)
                .build());
    }

    //
    // TODO: your code should also have tests for all types that you support and for the state machine of your cursor
    //

    //
    // Start http server for testing
    //

    @BeforeClass
    public void setUpClass()
            throws Exception {
        ourHttpServer = new OurHttpServer();
    }

    @AfterClass(alwaysRun = true)
    public void tearDown()
            throws Exception {
        if (ourHttpServer != null) {
            ourHttpServer.stop();
        }
    }
}
