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

package com.cyoda.connector.client.reporting;

import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class BaseReportsApiHandlerTest {

    @Test
    public void testReportName() {

        // Valid case
        assertEquals(BaseReportsApiHandler.toReportName("Cyoda-ReportingDecorator-Things"),"Things");

        // Weird, but valid case
        assertEquals(BaseReportsApiHandler.toReportName("-Things"),"Things");


        // Invalid case
        try {
            BaseReportsApiHandler.toReportName("Things");
            fail("should not get here");
        } catch (IllegalArgumentException e) {
            // All good
        }

        // Invalid case
        try {
            BaseReportsApiHandler.toReportName("");
            fail("should not get here");
        } catch (IllegalArgumentException e) {
            String message = e.getMessage();
            assertNotNull(message);
            assertTrue(message.length()>0);
        }

        // Invalid case
        try {
            BaseReportsApiHandler.toReportName("-");
            fail("should not get here");
        } catch (IllegalArgumentException e) {
            String message = e.getMessage();
            assertNotNull(message);
            assertTrue(message.length()>0);
        }

        // Invalid case
        try {
            //noinspection ConstantConditions
            BaseReportsApiHandler.toReportName(null);
            fail("should not get here");
        } catch (NullPointerException e) {
            String message = e.getMessage();
            assertNotNull(message);
            assertTrue(message.length()>0);
        }
    }

    public void testToTableName() {
        assertEquals(BaseReportsApiHandler.reportNameToTableName("My NameIsNobody"),"MY_NAME_IS_NOBODY");
        assertEquals(BaseReportsApiHandler.reportNameToTableName("$My Name-IsNo&bod%y"),"MY_NAME_IS_NOBODY");

        // Invalid case
        try {
            BaseReportsApiHandler.reportNameToTableName("-");
            fail("should not get here");
        } catch (IllegalArgumentException e) {
            String message = e.getMessage();
            assertNotNull(message);
            assertTrue(message.length()>0);
        }

        // Invalid case
        try {
            BaseReportsApiHandler.reportNameToTableName("");
            fail("should not get here");
        } catch (IllegalArgumentException e) {
            String message = e.getMessage();
            assertNotNull(message);
            assertTrue(message.length()>0);
        }

        // Invalid case
        try {
            //noinspection ConstantConditions
            BaseReportsApiHandler.reportNameToTableName(null);
            fail("should not get here");
        } catch (NullPointerException e) {
            String message = e.getMessage();
            assertNotNull(message);
            assertTrue(message.length()>0);
        }

    }
}