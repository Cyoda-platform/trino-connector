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
//
//package com.cyoda.connector;
//
//
//import com.cyoda.connector.handles.CyodaColumnHandle;
//import com.cyoda.connector.handles.CyodaTableMeta;
//import com.cyoda.connector.handles.CyodaTableLayoutHandle;
//import com.cyoda.connector.handles.CyodaTransactionHandle;
//import io.trino.spi.connector.ColumnHandle;
//import io.trino.spi.connector.ConnectorHandleResolver;
//import io.trino.spi.connector.ConnectorSplit;
//import io.trino.spi.connector.ConnectorTableHandle;
//import io.trino.spi.connector.ConnectorTableLayoutHandle;
//import io.trino.spi.connector.ConnectorTransactionHandle;
//
////TODO Interface is gone. Need to find out if this logic is necessary
//public class CyodaHandleResolver implements ConnectorHandleResolver {
//
//    @Override
//    public Class<? extends ConnectorTransactionHandle> getTransactionHandleClass() {
//        return CyodaTransactionHandle.class;
//    }
//
//    @Override
//    public Class<? extends ConnectorTableHandle> getTableHandleClass() {
//        return CyodaTableMeta.class;
//    }
//
//    @Override
//    public Class<? extends ConnectorTableLayoutHandle> getTableLayoutHandleClass() {
//        return CyodaTableLayoutHandle.class;
//    }
//
//    @Override
//    public Class<? extends ColumnHandle> getColumnHandleClass() {
//        return CyodaColumnHandle.class;
//    }
//
//    @Override
//    public Class<? extends ConnectorSplit> getSplitClass() {
//        return CyodaSplit.class;
//    }
//}
