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

package com.cyoda.presto.client.reporting.data;

import com.cyoda.presto.client.types.DataType;
import com.cyoda.presto.handles.CyodaColumnHandle;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.trino.spi.type.VarcharType;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.Map;

import static org.testng.Assert.*;

public class ReportRowNavigatorTest {
    
    private static Object testGetValue(String path, Map<String, Object> map){
        CyodaColumnHandle handle = new CyodaColumnHandle(path,
                VarcharType.VARCHAR, DataType.STRING, 0);
        return handle.getValue(map);
    }

    @Test
    public void testNormalColumnPath() {
        String path = "messageBody.content.lei";
        Map<String, Object> map = ImmutableMap.of(
                "messageBody", ImmutableMap.of(
                        "content", ImmutableMap.of(
                                "lei", "097900BFBV0000008117"
                        )
                ),
                "entityId", "04411004-8900-1000-8182-2841c0400c18",
                "creationDate", "2022-05-02T11:45:54.173+00:00"

        );
        Object value = testGetValue(path,map);
        assertEquals(value,"097900BFBV0000008117");
    }

    @Test
    public void testShortColumnPath() {
        String path = "creationDate";
        Map<String, Object> map = ImmutableMap.of(
                "creationDate", "2022-05-02T11:45:54.173+00:00"
        );
        Object value = testGetValue(path,map);
        assertEquals(value,"2022-05-02T11:45:54.173+00:00");
    }

    @Test
    public void testNoColumnPath() {
        String path = "";
        Map<String, Object> map = Collections.emptyMap();
        Object value = testGetValue(path,map);
        assertNull(value);
    }

    @Test
    public void testNoValueInColumnPathAtLeaf() {
        String path = "messageBody.content.lei";
        Map<String, Object> map = ImmutableMap.of(
                "messageBody", ImmutableMap.of(
                        "content", ImmutableMap.of(
                                "blah", "blubber"
                        )
                ),
                "entityId", "04411004-8900-1000-8182-2841c0400c18",
                "creationDate", "2022-05-02T11:45:54.173+00:00"

        );
        Object value = testGetValue(path,map);
        assertNull(value);
    }

    @Test
    public void testNoValueInColumnPath() {
        String path = "messageBody.content.lei";
        Map<String, Object> map = ImmutableMap.of(
                "entityId", "04411004-8900-1000-8182-2841c0400c18",
                "creationDate", "2022-05-02T11:45:54.173+00:00"

        );
        Object value = testGetValue(path,map);
        assertNull(value);
    }

    @Test
    public void testBrokenMap() {
        String path = "messageBody.content.lei";
        Map<String, Object> map = ImmutableMap.of(
                "messageBody", "this should be a map"
        );
        try {
            testGetValue(path,map);
            fail("should not get here");
        } catch (Exception e) {
            assertTrue(e.getMessage().length()>0);
        }
    }

    @Test
    public void testListWithStarAndOneMember() {
        String path = "org.people.[*].firstName";
        String firstPerson = "First Person";
        Map<String, Object> map = ImmutableMap.of(
                "org", ImmutableMap.of(
                        "people", ImmutableList.of(
                                ImmutableMap.of(
                                        "firstName", firstPerson,
                                        "nationality", "Latvian",
                                        "title", "director"
                                )
                        )
                ),
                "name", "ZLDS TRANSPORT LTD"
        );
        Object value = testGetValue(path,map);
        assertEquals(value,ImmutableList.of(firstPerson));
    }

    @Test
    public void testListWithIndexAndOneMember() {
        String path = "org.people.[0].firstName";
        String firstPerson = "First Person";
        Map<String, Object> map = ImmutableMap.of(
                "org", ImmutableMap.of(
                        "people", ImmutableList.of(
                                ImmutableMap.of(
                                        "firstName", firstPerson,
                                        "nationality", "Latvian",
                                        "title", "director"
                                )
                        )
                ),
                "name", "ZLDS TRANSPORT LTD"
        );
        Object value = testGetValue(path,map);
        assertEquals(value,firstPerson);
    }

    @Test
    public void testListWithStarAndSeveralMembers() {
        String path = "org.people.[*].firstName";
        String firstPerson = "First Person";
        String secondPerson = "Second Person";
        String thirdPerson = "Third Person";
        Map<String, Object> map = ImmutableMap.of(
                "org", ImmutableMap.of(
                        "people", ImmutableList.of(
                                ImmutableMap.of(
                                        "firstName", firstPerson,
                                        "nationality", "Latvian",
                                        "title", "director"
                                ),
                                ImmutableMap.of(
                                        "firstName", secondPerson,
                                        "nationality", "Latvian",
                                        "title", "director"
                                ),
                                ImmutableMap.of(
                                        "firstName", thirdPerson,
                                        "nationality", "Latvian",
                                        "title", "director"
                                )
                        )
                ),
                "name", "ZLDS TRANSPORT LTD"
        );
        Object value = testGetValue(path,map);
        assertEquals(value,ImmutableList.of(firstPerson,secondPerson,thirdPerson));
    }

    @Test
    public void testListWithIndexAndSeveralMembers() {
        String path = "org.people.[1].firstName";
        String firstPerson = "First Person";
        String secondPerson = "Second Person";
        String thirdPerson = "Third Person";
        Map<String, Object> map = ImmutableMap.of(
                "org", ImmutableMap.of(
                        "people", ImmutableList.of(
                                ImmutableMap.of(
                                        "firstName", firstPerson,
                                        "nationality", "Latvian",
                                        "title", "director"
                                ),
                                ImmutableMap.of(
                                        "firstName", secondPerson,
                                        "nationality", "Latvian",
                                        "title", "director"
                                ),
                                ImmutableMap.of(
                                        "firstName", thirdPerson,
                                        "nationality", "Latvian",
                                        "title", "director"
                                )
                        )
                ),
                "name", "ZLDS TRANSPORT LTD"
        );
        Object value = testGetValue(path,map);
        assertEquals(value,secondPerson);
    }

    @Test
    public void testListWithIndexOutOfBoundsAndSeveralMembers() {
        String path = "org.people.[67].firstName";
        String firstPerson = "First Person";
        String secondPerson = "Second Person";
        String thirdPerson = "Third Person";
        Map<String, Object> map = ImmutableMap.of(
                "org", ImmutableMap.of(
                        "people", ImmutableList.of(
                                ImmutableMap.of(
                                        "firstName", firstPerson,
                                        "nationality", "Latvian",
                                        "title", "director"
                                ),
                                ImmutableMap.of(
                                        "firstName", secondPerson,
                                        "nationality", "Latvian",
                                        "title", "director"
                                ),
                                ImmutableMap.of(
                                        "firstName", thirdPerson,
                                        "nationality", "Latvian",
                                        "title", "director"
                                )
                        )
                ),
                "name", "ZLDS TRANSPORT LTD"
        );
        try {
            testGetValue(path,map);
            fail("should not get here");
        } catch (Exception e) {
            assertTrue(e.getMessage().length()>0);
        }
    }
}