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
import com.cyoda.presto.handles.CyodaTableHandle;
import io.trino.spi.NodeManager;
import io.trino.spi.connector.ConnectorTableHandle;
import io.trino.spi.connector.Constraint;
import io.trino.spi.connector.DynamicFilter;
import io.trino.spi.connector.ConnectorSession;
import io.trino.spi.connector.ConnectorSplitSource;
import io.trino.spi.connector.ConnectorSplitManager;
import io.trino.spi.connector.ConnectorTransactionHandle;
import io.trino.spi.connector.FixedSplitSource;

import jakarta.inject.Inject;

import java.util.List;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

public class CyodaSplitManager implements ConnectorSplitManager {
    private final TableDataProviderProvider dataProviderProvider;
    private final AuthService auth;
    private final CyodaSplitDispatcher splitDispatcher;


    @Inject
    public CyodaSplitManager(TableDataProviderProvider dataProviderProvider,
                             AuthService auth,
                             NodeManager nodeManager) {
        this.dataProviderProvider = dataProviderProvider;
        this.auth = auth;
        splitDispatcher = new CyodaSplitDispatcher(nodeManager);
    }

    @Override
    public ConnectorSplitSource getSplits(
            ConnectorTransactionHandle handle,
            ConnectorSession session,
            ConnectorTableHandle connectorTableHandle,
            DynamicFilter dynamicFilter,
            Constraint constraint) {
        CyodaTableHandle tableHandle = (CyodaTableHandle) connectorTableHandle;

        if (tableHandle.getTableType().isPushdownSupported()){
            tableHandle.setConstraint(tableHandle.getConstraint().intersect(dynamicFilter.getCurrentPredicate()).simplify());
        }

        if (constraint.predicate().isEmpty() && !constraint.getSummary().isAll()){
            throw new RuntimeException("Constraint summary is not blank, but predicate not present");
        }
        List<CyodaSplit> splits = dataProviderProvider
                .getDataProvider(tableHandle.getTableType())
                .getSplits(auth.fromSession(session), session.getQueryId(), tableHandle, constraint);
        splitDispatcher.dispatch(splits);
        return new FixedSplitSource(splits);
    }
}
