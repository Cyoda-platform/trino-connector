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

package com.cyoda.presto.client.reporting;

import com.cyoda.presto.CyodaTable;
import com.facebook.presto.spi.SchemaTableName;
import com.google.common.base.Preconditions;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public abstract class AbstractTableHolder {

    @Nullable private Map<SchemaTableName, CyodaTable> tableMap = null;
    @Nullable private Map<TableDefinitionHandle, List<ColumnDefinition>> fieldDefs = null;
    @Nonnull protected final String endpoint;

    protected AbstractTableHolder(@Nonnull String endpoint) {
        this.endpoint = Preconditions.checkNotNull(endpoint,"endpoint is null");
    }

    private Map<SchemaTableName, CyodaTable> initTableMap(boolean refresh) {
        fieldDefs = refresh ? refreshFieldDefs() : lookupFieldDefs();
        tableMap = setupTableMap(endpoint,fieldDefs);
        return tableMap;
    }

    private Map<TableDefinitionHandle,List<ColumnDefinition>> lookupFieldDefs() {
        return Optional.ofNullable(fieldDefs).orElse(refreshFieldDefs());
    }

    protected final Map<SchemaTableName, CyodaTable> lookupTableMap() {
        return Optional.ofNullable(tableMap).orElse(initTableMap(false));
    }
    protected final Map<SchemaTableName, CyodaTable> refreshTableMap() {
        return initTableMap(true);
    }

    protected abstract Map<TableDefinitionHandle, List<ColumnDefinition>> refreshFieldDefs();
    abstract Map<SchemaTableName, CyodaTable> setupTableMap(String endpoint, Map<TableDefinitionHandle, List<ColumnDefinition>> fieldDefs);

    public static class TableDefinitionHandle {
        protected final String tableName;
        protected final String reportConfigurationId;

        public TableDefinitionHandle(String tableName) {
            this.tableName = tableName;
            this.reportConfigurationId = null;
        }

        public TableDefinitionHandle(String tableName, String reportConfigurationId) {
            this.tableName = tableName;
            this.reportConfigurationId = reportConfigurationId;
        }

        public static TableDefinitionHandle asTableDefinitionHandle(String tableName) {
            return new TableDefinitionHandle(tableName);
        }

        public static TableDefinitionHandle asTableDefinitionHandle(String tableName,String reportConfigurationId) {
            return new TableDefinitionHandle(tableName,reportConfigurationId);
        }
    }
}
