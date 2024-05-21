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

import com.cyoda.presto.auth.AuthService;
import com.cyoda.presto.client.data.TableDataProviderProvider;
import com.cyoda.presto.handles.CyodaColumnHandle;
import com.cyoda.presto.handles.CyodaTableHandle;
import com.cyoda.presto.handles.CyodaTableMeta;
import com.cyoda.presto.logging.SupplierLogger;
import io.trino.spi.connector.ConnectorTableHandle;
import io.trino.spi.connector.DynamicFilter;
import io.trino.spi.connector.ColumnHandle;
import io.trino.spi.connector.ConnectorPageSource;
import io.trino.spi.connector.ConnectorSession;
import io.trino.spi.connector.ConnectorSplit;
import io.trino.spi.connector.ConnectorPageSourceProvider;
import io.trino.spi.connector.ConnectorTransactionHandle;
import com.google.common.base.Preconditions;

import jakarta.inject.Inject;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

public class CyodaPageSourceProvider implements ConnectorPageSourceProvider {

    private static final SupplierLogger LOG = SupplierLogger.get(CyodaPageSourceProvider.class);
    private final TableDataProviderProvider dataProviderProvider;
    private final CyodaMetadata cyodaMetadata;

    @Inject
    public CyodaPageSourceProvider(CyodaConnectorId connectorId,
                                   TableDataProviderProvider dataProviderProvider,
                                   CyodaMetadata cyodaMetadata) {
        this.dataProviderProvider = requireNonNull(dataProviderProvider, "dataProviderProvider is null");
        this.cyodaMetadata = cyodaMetadata;
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
        CyodaTableHandle handle = (CyodaTableHandle) tableHandle;
        CyodaTableMeta cyodaTableMeta = cyodaMetadata.getTableMeta(handle);
        CyodaSplit cyodaSplit = (CyodaSplit) split;
        if (handle.getTableType().isPushdownSupported()){
            if (dynamicFilter.isAwaitable()) {
                int cnt = 0;
                while (!dynamicFilter.isComplete() && cnt++ < 100) try {
                    Thread.sleep(100);
                } catch (InterruptedException e){
                    throw new RuntimeException(e);
                }
                if (dynamicFilter.isComplete()){
                    cyodaSplit.setConstraint(cyodaSplit.getConstraint().intersect(dynamicFilter.getCurrentPredicate()));
                } else LOG.error("Exceeded 10s timeout for dynamic filter await");
            } else {
                cyodaSplit.setConstraint(cyodaSplit.getConstraint().intersect(dynamicFilter.getCurrentPredicate()));
            }
        }
        List<CyodaColumnHandle> cyodaColumns = columns.stream().map(CyodaColumnHandle.class::cast).collect(Collectors.toList());

        return dataProviderProvider
                .getDataProvider(cyodaTableMeta.getTableType())
                .getPageSource(cyodaTableMeta, cyodaColumns, cyodaSplit);

    }
}
