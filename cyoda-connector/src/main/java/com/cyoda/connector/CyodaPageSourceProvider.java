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

package com.cyoda.connector;

import com.cyoda.connector.client.data.TableDataProviderProvider;
import com.cyoda.connector.client.reporting.stats.ConditionPushdownLogMonitor;
import com.cyoda.connector.handles.CyodaColumnHandle;
import com.cyoda.connector.handles.CyodaTableHandle;
import com.cyoda.connector.handles.CyodaTableMeta;
import com.cyoda.connector.logging.SupplierLogger;
import io.trino.spi.connector.ConnectorTableHandle;
import io.trino.spi.connector.DynamicFilter;
import io.trino.spi.connector.ColumnHandle;
import io.trino.spi.connector.ConnectorPageSource;
import io.trino.spi.connector.ConnectorSession;
import io.trino.spi.connector.ConnectorSplit;
import io.trino.spi.connector.ConnectorPageSourceProvider;
import io.trino.spi.connector.ConnectorTransactionHandle;

import io.trino.spi.predicate.TupleDomain;
import jakarta.inject.Inject;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

public class CyodaPageSourceProvider implements ConnectorPageSourceProvider {

    private static final SupplierLogger LOG = SupplierLogger.get(CyodaPageSourceProvider.class);
    private final TableDataProviderProvider dataProviderProvider;
    private final CyodaMetadata cyodaMetadata;
    private final ConditionPushdownLogMonitor pushdownLogMonitor;

    @Inject
    public CyodaPageSourceProvider(TableDataProviderProvider dataProviderProvider,
                                   CyodaMetadata cyodaMetadata, ConditionPushdownLogMonitor pushdownLogMonitor) {
        this.dataProviderProvider = requireNonNull(dataProviderProvider, "dataProviderProvider is null");
        this.cyodaMetadata = cyodaMetadata;
        this.pushdownLogMonitor = pushdownLogMonitor;
    }

    @Override
    public ConnectorPageSource createPageSource(
            ConnectorTransactionHandle transactionHandle,
            ConnectorSession session,
            ConnectorSplit split,
            ConnectorTableHandle tableHandle,
            List<ColumnHandle> columns,
            DynamicFilter dynamicFilter
    ) {
        requireNonNull(split, "split is null");
        CyodaSplit cyodaSplit = (CyodaSplit) split;
        CyodaTableHandle handle = cyodaSplit.getTableHandle();
        CyodaTableMeta cyodaTableMeta = cyodaMetadata.getTableMeta(handle);
        if (handle.getTableType().isPushdownSupported()){
            if (dynamicFilter.isAwaitable()) {
                int cnt = 0;
                while (!dynamicFilter.isComplete() && cnt++ < 100) try {
                    Thread.sleep(100);
                } catch (InterruptedException e){
                    throw new RuntimeException(e);
                }
                if (dynamicFilter.isComplete()){
                    setConstraintToHandle(session, dynamicFilter, handle);
                } else LOG.error("Exceeded 10s timeout for dynamic filter await");
            } else {
                setConstraintToHandle(session, dynamicFilter, handle);
            }
        }
        List<CyodaColumnHandle> cyodaColumns = columns.stream()
                .map(CyodaColumnHandle.class::cast)
                .sorted(Comparator.comparingInt(CyodaColumnHandle::getOrdinalPosition))
                .collect(Collectors.toList());

        return dataProviderProvider
                .getDataProvider(cyodaTableMeta.getTableType())
                .getPageSource(cyodaTableMeta, cyodaColumns, cyodaSplit);

    }

    private void setConstraintToHandle(ConnectorSession session, DynamicFilter dynamicFilter, CyodaTableHandle handle) {
        TupleDomain<ColumnHandle> currentPredicate = dynamicFilter.getCurrentPredicate();

        handle.setConstraint(handle.getConstraint().intersect(currentPredicate));
    }
}
