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

package com.cyoda.presto.client.jodabeans;

import com.cyoda.presto.client.reporting.ColumnDefinition;
import com.cyoda.presto.handles.CyodaColumnHandle;
import com.cyoda.service.api.beans.GroupHeader;
import com.facebook.presto.common.type.StandardTypes;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.cyoda.presto.client.types.DataType.STRING;
import static org.testng.Assert.*;

public class StandardColumnDefinitionTest {

    @Test
    public void testMixed() {
        final List<ColumnDefinition> defs = StandardColumnDefinition.builder()
                .add(new StandardColumnDefinition(0, "varcharColumn", StandardTypes.VARCHAR, STRING, null, null))
                .add(new StandardColumnDefinition(0, "anotherVarcharColumn", StandardTypes.VARCHAR, STRING, null, null))
                .add(GroupHeader.meta())
                .build();
        try {
            Map<Integer, ColumnDefinition> colMap = defs.stream().collect(Collectors.toMap(ColumnDefinition::getPos, x -> x));
            assertEquals(colMap.size(),GroupHeader.meta().metaPropertyMap().size()+2);
        } catch (Exception e) {
            fail("probably have duplicate positions",e);
        }
    }

    @Test
    public void testSingle() {
        final List<ColumnDefinition> defs = StandardColumnDefinition.builder()
                .add(new StandardColumnDefinition(0, "varcharColumn", StandardTypes.VARCHAR, STRING, null, null))
                .add(new StandardColumnDefinition(0, "anotherVarcharColumn", StandardTypes.VARCHAR, STRING, null, null))
                .build();
        try {
            Map<Integer, ColumnDefinition> colMap = defs.stream().collect(Collectors.toMap(ColumnDefinition::getPos, x -> x));
            assertEquals(colMap.size(),2);
        } catch (Exception e) {
            fail("probably have duplicate positions",e);
        }
    }

    @Test
    public void testJodaBean() {
        final List<ColumnDefinition> defs = StandardColumnDefinition.builder()
                .add(GroupHeader.meta())
                .build();
        try {
            Map<Integer, ColumnDefinition> colMap = defs.stream().collect(Collectors.toMap(ColumnDefinition::getPos, x -> x));
            assertEquals(colMap.size(),GroupHeader.meta().metaPropertyMap().size());
        } catch (Exception e) {
            fail("probably have duplicate positions",e);
        }
    }
}