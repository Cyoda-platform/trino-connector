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

package com.cyoda.presto.logging;

import io.trino.spi.eventlistener.QueryStatistics;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Duration;

public class LoggableQueryStatistics {
    private final Duration cpuTime;
    private final Duration failedCpuTime;
    private final Duration wallTime;
//    private final Duration waitingForPrerequisitesTime;
    private final Duration queuedTime;
    private final Duration waitingForResourcesTime;
//    private final Duration semanticAnalyzingTime;
//    private final Duration columnAccessPermissionCheckingTime;
//    private final Duration dispatchingTime;
    private final Duration planningTime;
    private final Duration analysisTime;
    private final Duration executionTime;

//    private final int peakRunningTasks;
    private final long peakUserMemoryBytes;
    // peak of user + system memory
//    private final long peakTotalNonRevocableMemoryBytes;
    private final long peakTaskUserMemory;
    private final long peakTaskTotalMemory;
//    private final long peakNodeTotalMemory;
    private final long totalBytes;
    private final long totalRows;
    private final long outputBytes;
    private final long outputRows;
    private final long writtenOutputBytes;
    private final long writtenOutputRows;
//    private final long writtenIntermediateBytes;
//    private final long spilledBytes;

    private final double cumulativeMemory;
//    private final double cumulativeTotalMemory;

    private final int completedSplits;
    private final boolean complete;

    public static LoggableQueryStatistics from(QueryStatistics stats) {
        return new LoggableQueryStatistics(
                stats.getCpuTime(),
                stats.getFailedCpuTime(),
                stats.getWallTime(),
//                stats.getWaitingForPrerequisitesTime(),
                stats.getQueuedTime(),
                stats.getResourceWaitingTime().orElse(null),
//                stats.getSemanticAnalyzingTime(),
//                stats.getColumnAccessPermissionCheckingTime(),
//                stats.getDispatchingTime(),
                stats.getPlanningTime().orElse(null),
                stats.getAnalysisTime().orElse(null),
                stats.getExecutionTime().orElse(null),
//                stats.getPeakRunningTasks(),
                stats.getPeakUserMemoryBytes(),
//                stats.getPeakTotalNonRevocableMemoryBytes(),
                stats.getPeakTaskUserMemory(),
                stats.getPeakTaskTotalMemory(),
//                stats.getPeakNodeTotalMemory(),
                stats.getTotalBytes(),
                stats.getTotalRows(),
                stats.getOutputBytes(),
                stats.getOutputRows(),
                stats.getWrittenBytes(),
                stats.getWrittenRows(),
//                stats.getWrittenIntermediateBytes(),
//                stats.getSpilledBytes(),
                stats.getCumulativeMemory(),
//                stats.getCumulativeTotalMemory(),
                stats.getCompletedSplits(),
                stats.isComplete()
        );
    }
    
    public LoggableQueryStatistics(
             Duration cpuTime,
             Duration failedCpuTime,
             Duration wallTime,
//             Duration waitingForPrerequisitesTime,
             Duration queuedTime,
             Duration waitingForResourcesTime,
//             Duration semanticAnalyzingTime,
//             Duration columnAccessPermissionCheckingTime,
//             Duration dispatchingTime,
             Duration planningTime,
             Duration analysisTime,
             Duration executionTime,
//             int peakRunningTasks,
             long peakUserMemoryBytes,
//             long peakTotalNonRevocableMemoryBytes,
             long peakTaskUserMemory,
             long peakTaskTotalMemory,
//             long peakNodeTotalMemory,
             long totalBytes,
             long totalRows,
             long outputBytes,
             long outputRows,
             long writtenOutputBytes,
             long writtenOutputRows,
//             long writtenIntermediateBytes,
//             long spilledBytes,
             double cumulativeMemory,
//             double cumulativeTotalMemory,
             int completedSplits,
             boolean complete
    ) {
        this.cpuTime = cpuTime;
        this.failedCpuTime = failedCpuTime;
        this.wallTime = wallTime;
//        this.waitingForPrerequisitesTime = waitingForPrerequisitesTime;
        this.queuedTime = queuedTime;
        this.waitingForResourcesTime = waitingForResourcesTime;
//        this.semanticAnalyzingTime = semanticAnalyzingTime;
//        this.columnAccessPermissionCheckingTime = columnAccessPermissionCheckingTime;
//        this.dispatchingTime = dispatchingTime;
        this.planningTime = planningTime;
        this.analysisTime = analysisTime;
        this.executionTime = executionTime;
//        this.peakRunningTasks = peakRunningTasks;
        this.peakUserMemoryBytes = peakUserMemoryBytes;
//        this.peakTotalNonRevocableMemoryBytes = peakTotalNonRevocableMemoryBytes;
        this.peakTaskUserMemory = peakTaskUserMemory;
        this.peakTaskTotalMemory = peakTaskTotalMemory;
//        this.peakNodeTotalMemory = peakNodeTotalMemory;
        this.totalBytes = totalBytes;
        this.totalRows = totalRows;
        this.outputBytes = outputBytes;
        this.outputRows = outputRows;
        this.writtenOutputBytes = writtenOutputBytes;
        this.writtenOutputRows = writtenOutputRows;
//        this.writtenIntermediateBytes = writtenIntermediateBytes;
//        this.spilledBytes = spilledBytes;
        this.cumulativeMemory = cumulativeMemory;
//        this.cumulativeTotalMemory = cumulativeTotalMemory;
        this.completedSplits = completedSplits;
        this.complete = complete;
    }

    @JsonProperty
    public Duration getCpuTime() {
        return cpuTime;
    }

    @JsonProperty
    public Duration getFailedCpuTime() {
        return failedCpuTime;
    }

    @JsonProperty
    public Duration getWallTime() {
        return wallTime;
    }

//    @JsonProperty
//    public Duration getWaitingForPrerequisitesTime() {
//        return waitingForPrerequisitesTime;
//    }

    @JsonProperty
    public Duration getQueuedTime() {
        return queuedTime;
    }

    @JsonProperty
    public Duration getWaitingForResourcesTime() {
        return waitingForResourcesTime;
    }

//    @JsonProperty
//    public Duration getSemanticAnalyzingTime() {
//        return semanticAnalyzingTime;
//    }
//
//    @JsonProperty
//    public Duration getColumnAccessPermissionCheckingTime() {
//        return columnAccessPermissionCheckingTime;
//    }
//
//    @JsonProperty
//    public Duration getDispatchingTime() {
//        return dispatchingTime;
//    }

    @JsonProperty
    public Duration getPlanningTime() {
        return planningTime;
    }

    @JsonProperty
    public Duration getAnalysisTime() {
        return analysisTime;
    }

    @JsonProperty
    public Duration getExecutionTime() {
        return executionTime;
    }
//
//    @JsonProperty
//    public int getPeakRunningTasks() {
//        return peakRunningTasks;
//    }

    @JsonProperty
    public long getPeakUserMemoryBytes() {
        return peakUserMemoryBytes;
    }
//
//    @JsonProperty
//    public long getPeakTotalNonRevocableMemoryBytes() {
//        return peakTotalNonRevocableMemoryBytes;
//    }

    @JsonProperty
    public long getPeakTaskUserMemory() {
        return peakTaskUserMemory;
    }

    @JsonProperty
    public long getPeakTaskTotalMemory() {
        return peakTaskTotalMemory;
    }
//
//    @JsonProperty
//    public long getPeakNodeTotalMemory() {
//        return peakNodeTotalMemory;
//    }

    @JsonProperty
    public long getTotalBytes() {
        return totalBytes;
    }

    @JsonProperty
    public long getTotalRows() {
        return totalRows;
    }

    @JsonProperty
    public long getOutputBytes() {
        return outputBytes;
    }

    @JsonProperty
    public long getOutputRows() {
        return outputRows;
    }

    @JsonProperty
    public long getWrittenOutputBytes() {
        return writtenOutputBytes;
    }

    @JsonProperty
    public long getWrittenOutputRows() {
        return writtenOutputRows;
    }
//
//    @JsonProperty
//    public long getWrittenIntermediateBytes() {
//        return writtenIntermediateBytes;
//    }
//
//    @JsonProperty
//    public long getSpilledBytes() {
//        return spilledBytes;
//    }

    @JsonProperty
    public double getCumulativeMemory() {
        return cumulativeMemory;
    }
//
//    @JsonProperty
//    public double getCumulativeTotalMemory() {
//        return cumulativeTotalMemory;
//    }

    @JsonProperty
    public int getCompletedSplits() {
        return completedSplits;
    }

    @JsonProperty
    public boolean isComplete() {
        return complete;
    }

}
