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
import com.cyoda.presto.handles.DummyTableHandle;
import com.cyoda.presto.logging.SupplierLogger;
import io.trino.spi.Page;
import io.trino.spi.PageBuilder;
import io.trino.spi.block.BlockBuilder;
import io.trino.spi.connector.ConnectorPageSource;

import java.util.Collections;
import java.util.List;

import static com.google.common.collect.ImmutableList.toImmutableList;

public class DummyPageSource implements ConnectorPageSource
{
    private static final SupplierLogger LOG = SupplierLogger.get(DummyPageSource.class);

    private boolean finished;

    private final Page page;

    public DummyPageSource(DummyTableHandle tableHandle) {
        this.finished = false;
        List<CyodaColumnHandle> columnHandles = tableHandle.getProjectedColumns();
        PageBuilder pageBuilder = new PageBuilder(
                columnHandles.stream()
                        .map(CyodaColumnHandle::getColumnType)
                        .collect(toImmutableList())
        );

        for (int i = 0; i < columnHandles.size(); i++) {
            CyodaColumnHandle columnHandle = columnHandles.get(i);
            BlockBuilder blockBuilder = pageBuilder.getBlockBuilder(i);
            columnHandle.writeValue(blockBuilder, tableHandle.getContent().get(columnHandle.getColumnName()));
        }

        pageBuilder.declarePosition();
        page = pageBuilder.build();

    }

    @Override
    public long getCompletedBytes()
    {
        return 0;
    }

    @Override
    public long getReadTimeNanos() {
        return 0;
    }

    @Override
    public boolean isFinished()
    {
        return finished;
    }

    @Override
    public Page getNextPage() {
        if (finished) {
            return null;
        } else {
            finished = true;
            return page;
        }
    }

    @Override
    public long getMemoryUsage() {
        return 0;
    }

    @Override
    public void close()
    {
        finished = true;
    }

}
