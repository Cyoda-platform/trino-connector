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

import io.trino.spi.eventlistener.SplitStatistics;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Duration;
import java.util.Optional;

public class LoggableSplitStatistics {
    private final Duration cpuTime;
    private final Duration wallTime;
    private final Duration queuedTime;
    private final Duration completedReadTime;

    private final long completedPositions;
    private final long completedDataSizeBytes;

    private final Duration timeToFirstByte;
    private final Duration timeToLastByte;

    public static LoggableSplitStatistics from(SplitStatistics stats) {
        return new LoggableSplitStatistics(
                stats.getCpuTime(),
                stats.getWallTime(),
                stats.getQueuedTime(),
                stats.getCompletedReadTime(),
                stats.getCompletedPositions(),
                stats.getCompletedDataSizeBytes(),
                stats.getTimeToFirstByte().orElse(null),
                stats.getTimeToLastByte().orElse(null)
        );
    }
    public LoggableSplitStatistics(
             Duration cpuTime,
             Duration wallTime,
             Duration queuedTime,
             Duration completedReadTime,
             long completedPositions,
             long completedDataSizeBytes,
             Duration timeToFirstByte,
              Duration timeToLastByte
    ) {
        this.cpuTime = cpuTime;
        this.wallTime = wallTime;
        this.queuedTime = queuedTime;
        this.completedReadTime = completedReadTime;
        this.completedPositions = completedPositions;
        this.completedDataSizeBytes = completedDataSizeBytes;
        this.timeToFirstByte = timeToFirstByte;
        this.timeToLastByte = timeToLastByte;
    }

    @JsonProperty
    public Duration getCpuTime() {
        return cpuTime;
    }

    @JsonProperty
    public Duration getWallTime() {
        return wallTime;
    }

    @JsonProperty
    public Duration getQueuedTime() {
        return queuedTime;
    }

    @JsonProperty
    public Duration getCompletedReadTime() {
        return completedReadTime;
    }

    @JsonProperty
    public long getCompletedPositions() {
        return completedPositions;
    }

    @JsonProperty
    public long getCompletedDataSizeBytes() {
        return completedDataSizeBytes;
    }

    @JsonProperty
    public Duration getTimeToFirstByte() {
        return timeToFirstByte;
    }

    @JsonProperty
    public Duration getTimeToLastByte() {
        return timeToLastByte;
    }
}
