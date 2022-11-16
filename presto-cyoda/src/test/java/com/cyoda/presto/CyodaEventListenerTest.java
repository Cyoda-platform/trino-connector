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

import com.cyoda.presto.logging.SupplierLogger;
import io.trino.spi.eventlistener.QueryCompletedEvent;
import io.trino.spi.eventlistener.QueryCreatedEvent;
import io.trino.spi.eventlistener.QueryFailureInfo;
import io.trino.spi.eventlistener.QueryMetadata;
import io.trino.spi.eventlistener.QueryStatistics;
import io.trino.spi.eventlistener.SplitCompletedEvent;
import io.trino.spi.eventlistener.SplitStatistics;
import com.google.common.collect.ImmutableMap;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.testng.annotations.Test;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static com.cyoda.presto.CyodaEventListener.*;
import static org.jeasy.random.FieldPredicates.*;
import static org.mockito.Mockito.*;

public class CyodaEventListenerTest {

    EasyRandom baseRandom = new EasyRandom();
    EasyRandom easyRandom = new EasyRandom(
            new EasyRandomParameters()
                    .randomize(Optional.class,() -> Optional.of(baseRandom.nextObject(String.class)))
                    .randomize(ofType(Optional.class).and(inClass(QueryStatistics.class)),
                            ()->Optional.of(baseRandom.nextObject(Duration.class)))
                    .randomize(named("planNodeStatsAndCosts").and(ofType(Optional.class)).and(inClass(QueryStatistics.class)),
                            ()->Optional.of(baseRandom.nextObject(String.class)))
                    .randomize(ofType(Optional.class).and(inClass(SplitStatistics.class)),
                            ()->Optional.of(baseRandom.nextObject(Duration.class)))
    );

    @Test
    public void testQueryCreated_DebugDisabled() {
        Map<String,String> config = setupConfig(false,false,false,false);

        QueryCreatedEvent mockQueryCreatedEvent = mock(QueryCreatedEvent.class);
        QueryMetadata randomMetaData = easyRandom.nextObject(QueryMetadata.class);

        SupplierLogger mockLogger = mock(SupplierLogger.class);
        when(mockLogger.isDebugEnabled()).thenReturn(true);

        CyodaEventListener listener = new CyodaEventListener(config,mockLogger);
        listener.queryCreated(mockQueryCreatedEvent);
        verify(mockLogger,never()).error(any(),any());
        verify(mockLogger,never()).warn(anyString(),any());
        verify(mockLogger,never()).debug(anyString(),anyString());
        verify(mockLogger,never()).debug(anyString());
    }

    @Test
    public void testQueryCreated_MetaEnabled() {
        Map<String,String> config = setupConfig(true,false,false,false);

        int debugCalled = 1;
        doQueryCreated(config, debugCalled);
    }

    @Test
    public void testQueryCreated_ContextEnabled() {
        Map<String,String> config = setupConfig(false,true,false,false);

        int debugCalled = 1;
        doQueryCreated(config, debugCalled);
    }

    @Test
    public void testQueryCreated_BothEnabled() {
        Map<String,String> config = setupConfig(true,true,false,false);
        int debugCalled = 2;
        doQueryCreated(config, debugCalled);
    }

    private void doQueryCreated(Map<String, String> config, int debugCalled) {
        QueryCreatedEvent mockQueryCreatedEvent = mock(QueryCreatedEvent.class);
        QueryMetadata randomMetaData = easyRandom.nextObject(QueryMetadata.class);
        when(mockQueryCreatedEvent.getMetadata()).thenReturn(randomMetaData);

        SupplierLogger mockLogger = mock(SupplierLogger.class);
        when(mockLogger.isDebugEnabled()).thenReturn(true);

        CyodaEventListener listener = new CyodaEventListener(config,mockLogger);
        listener.queryCreated(mockQueryCreatedEvent);
        verify(mockLogger,never()).error(any(),any());
        verify(mockLogger,never()).warn(anyString(),any());
        verify(mockLogger,times(debugCalled)).debug(anyString(),anyString());
        verify(mockLogger,never()).debug(anyString());
    }

    @Test
    public void testSplitCompleted_DebugDisabled() {
        Map<String,String> config = setupConfig(true,true,true,true);

        int debugCalled = 1;
        boolean debugEnabled = false;
        doSplitCompleted(config, debugEnabled, debugCalled);
    }

    @Test
    public void testSplitCompleted_DebugEnabled_SplitLoggingDisabled() {
        Map<String,String> config = setupConfig(true,true,false,true);

        int debugCalled = 1;
        boolean debugEnabled = true;
        doSplitCompleted(config, debugEnabled, debugCalled);
    }

    @Test
    public void testSplitCompleted_DebugEnabled_SplitLoggingEnabled() {
        Map<String,String> config = setupConfig(true,true,true,true);

        int debugCalled = 2;
        boolean debugEnabled = true;
        doSplitCompleted(config, debugEnabled, debugCalled);
    }

    private void doSplitCompleted(Map<String, String> config, boolean debugEnabled, int debugCalled) {
        SplitCompletedEvent mockSplitCompletedEvent = mock(SplitCompletedEvent.class);
        when(mockSplitCompletedEvent.getQueryId()).thenReturn("query-id");
        SplitStatistics randomSplitStatistics = easyRandom.nextObject(SplitStatistics.class);
        when(mockSplitCompletedEvent.getStatistics()).thenReturn(randomSplitStatistics);

        SupplierLogger mockLogger = mock(SupplierLogger.class);
        when(mockLogger.isDebugEnabled()).thenReturn(debugEnabled);

        CyodaEventListener listener = new CyodaEventListener(config,mockLogger);
        listener.splitCompleted(mockSplitCompletedEvent);
        verify(mockLogger,never()).error(any(),any());
        verify(mockLogger,never()).warn(anyString(),any());
        verify(mockLogger,times(debugCalled)).debug(anyString(),anyString());
        verify(mockLogger,never()).debug(anyString());
    }



    @Test
    public void testQueryCompleted_withFailure_DebugDisabled() {
        Map<String,String> config = setupConfig(false,false,false,false);

        QueryCompletedEvent mockQueryCompletedEvent = mock(QueryCompletedEvent.class);
//        when(mockQueryCompletedEvent.getFailedTasks()).thenReturn(Collections.singletonList("Failed!"));
        QueryMetadata mockMetaData = mock(QueryMetadata.class);
        when(mockMetaData.getQueryId()).thenReturn("my-query-id");
        when(mockQueryCompletedEvent.getMetadata()).thenReturn(mockMetaData);

        QueryFailureInfo randomeQueryFailureInfo = easyRandom.nextObject(QueryFailureInfo.class);
        when(mockQueryCompletedEvent.getFailureInfo()).thenReturn(Optional.of(randomeQueryFailureInfo));

        SupplierLogger mockLogger = mock(SupplierLogger.class);
        when(mockLogger.isDebugEnabled()).thenReturn(false);

        CyodaEventListener listener = new CyodaEventListener(config,mockLogger);
        listener.queryCompleted(mockQueryCompletedEvent);
        verify(mockLogger,never()).error(any(),any());
        verify(mockLogger,times(1)).warn(anyString(),anyString());
        verify(mockLogger,times(1)).debug(anyString(),anyString());
        verify(mockLogger,never()).debug(anyString());
    }

    @Test
    public void testQueryCompleted_DebugDisabled() {
        Map<String,String> config = setupConfig(false,false,false,false);

        QueryCompletedEvent mockQueryCompletedEvent = mock(QueryCompletedEvent.class);
//        when(mockQueryCompletedEvent.getFailedTasks()).thenReturn(Collections.emptyList());
        QueryMetadata mockMetaData = mock(QueryMetadata.class);
        when(mockMetaData.getQueryId()).thenReturn("my-query-id");
        when(mockQueryCompletedEvent.getMetadata()).thenReturn(mockMetaData);

        when(mockQueryCompletedEvent.getFailureInfo()).thenReturn(Optional.empty());

        SupplierLogger mockLogger = mock(SupplierLogger.class);
        when(mockLogger.isDebugEnabled()).thenReturn(true);

        CyodaEventListener listener = new CyodaEventListener(config,mockLogger);
        listener.queryCompleted(mockQueryCompletedEvent);
        verify(mockLogger,never()).error(any(),any());
        verify(mockLogger,never()).warn(anyString(),any());
        verify(mockLogger,times(1)).debug(anyString(),anyString());
        verify(mockLogger,never()).debug(anyString());
    }

    @Test
    public void testQueryCompleted_DebugEnabled() {
        Map<String,String> config = setupConfig(true,true,true,true);

        QueryCompletedEvent mockQueryCompletedEvent = mock(QueryCompletedEvent.class);
//        when(mockQueryCompletedEvent.getFailedTasks()).thenReturn(Collections.emptyList());
        QueryMetadata mockMetaData = mock(QueryMetadata.class);
        when(mockMetaData.getQueryId()).thenReturn("my-query-id");
        when(mockQueryCompletedEvent.getMetadata()).thenReturn(mockMetaData);

        when(mockQueryCompletedEvent.getFailureInfo()).thenReturn(Optional.empty());
        when(mockQueryCompletedEvent.getStatistics()).thenReturn(easyRandom.nextObject(QueryStatistics.class));

        SupplierLogger mockLogger = mock(SupplierLogger.class);
        when(mockLogger.isDebugEnabled()).thenReturn(true);

        CyodaEventListener listener = new CyodaEventListener(config,mockLogger);
        listener.queryCompleted(mockQueryCompletedEvent);
        verify(mockLogger,never()).error(any(),any());
        verify(mockLogger,never()).warn(anyString(),any());
        verify(mockLogger,never()).debug(anyString());
        verify(mockLogger,times(2)).debug(anyString(),anyString());
    }

    @Test
    public void testQueryCompleted_WithFails_DebugEnabled() {
        Map<String,String> config = setupConfig(true,true,true,true);

        QueryCompletedEvent mockQueryCompletedEvent = mock(QueryCompletedEvent.class);
//        when(mockQueryCompletedEvent.getFailedTasks()).thenReturn(Collections.singletonList("Failed!"));
        QueryMetadata mockMetaData = mock(QueryMetadata.class);
        when(mockMetaData.getQueryId()).thenReturn("my-query-id");
        when(mockQueryCompletedEvent.getMetadata()).thenReturn(mockMetaData);

        QueryFailureInfo randomeQueryFailureInfo = easyRandom.nextObject(QueryFailureInfo.class);
        when(mockQueryCompletedEvent.getFailureInfo()).thenReturn(Optional.of(randomeQueryFailureInfo));
        when(mockQueryCompletedEvent.getStatistics()).thenReturn(easyRandom.nextObject(QueryStatistics.class));

        SupplierLogger mockLogger = mock(SupplierLogger.class);
        when(mockLogger.isDebugEnabled()).thenReturn(true);

        CyodaEventListener listener = new CyodaEventListener(config,mockLogger);
        listener.queryCompleted(mockQueryCompletedEvent);
        verify(mockLogger,never()).error(any(),any());
        verify(mockLogger,times(1)).warn(anyString(),any());
        verify(mockLogger,never()).debug(anyString());
        verify(mockLogger,atLeastOnce()).debug(anyString(),anyString());
    }

    private ImmutableMap<String, String> setupConfig(boolean logMeta, boolean logContext, boolean logSplits, boolean logStats) {
        return ImmutableMap.of(
                QUERY_META_LOGGING_FLAG, logMeta ? "1" : "0",
                QUERY_CONTEXT_LOGGING_FLAG, logContext ? "1" : "0",
                QUERY_SPLIT_LOGGING_FLAG, logSplits ? "1" : "0",
                QUERY_STATS_LOGGING_FLAG, logStats ? "1" : "0"
        );
    }
}